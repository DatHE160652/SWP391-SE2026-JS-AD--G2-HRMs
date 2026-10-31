package com.hrms.dao;

import com.hrms.model.ResponsibilityLevel;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponsibilityLevelDAO {
    private static final String SELECT = """
            SELECT r.*,(SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.Amount) END
                FROM ResponsibilityAllowancePolicies a WHERE a.CompanyId=r.CompanyId
                AND a.ResponsibilityLevelId=r.ResponsibilityLevelId
                AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))) AS DefaultAllowance
            FROM ResponsibilityLevels r
            """;
    public List<ResponsibilityLevel> getLevels(int companyId) throws SQLException {
        try (Connection connection=DBConnection.getConnection();
             PreparedStatement statement=connection.prepareStatement(SELECT+" WHERE r.CompanyId=? ORDER BY r.RankOrder,r.ResponsibilityLevelId")) {
            statement.setInt(1,companyId);
            try (ResultSet rows=statement.executeQuery()) {
                List<ResponsibilityLevel> levels=new ArrayList<>();
                while (rows.next()) levels.add(map(rows));
                return levels;
            }
        }
    }

    public ResponsibilityLevel getLevel(int companyId,int levelId) throws SQLException {
        try (Connection connection=DBConnection.getConnection();
             PreparedStatement statement=connection.prepareStatement(SELECT+" WHERE r.CompanyId=? AND r.ResponsibilityLevelId=?")) {
            statement.setInt(1,companyId); statement.setInt(2,levelId);
            try (ResultSet rows=statement.executeQuery()) { return rows.next()?map(rows):null; }
        }
    }

    public boolean valueExists(int companyId,String code,Integer rank,Integer excludeId) throws SQLException {
        try (Connection connection=DBConnection.getConnection();
             PreparedStatement statement=connection.prepareStatement("""
                     SELECT 1 FROM ResponsibilityLevels WHERE CompanyId=?
                     AND ((? IS NOT NULL AND LevelCode=?) OR (? IS NOT NULL AND RankOrder=?))
                     AND (? IS NULL OR ResponsibilityLevelId<>?)
                     """)) {
            statement.setInt(1,companyId); statement.setString(2,code); statement.setString(3,code);
            statement.setObject(4,rank,Types.INTEGER); statement.setObject(5,rank,Types.INTEGER);
            statement.setObject(6,excludeId,Types.INTEGER); statement.setObject(7,excludeId,Types.INTEGER);
            try (ResultSet rows=statement.executeQuery()) { return rows.next(); }
        }
    }

    public boolean insertLevel(ResponsibilityLevel level) throws SQLException {
        return OrganizationTransaction.execute(level.getCompanyId(), connection -> {
            try (PreparedStatement statement=connection.prepareStatement("""
                     INSERT INTO ResponsibilityLevels(CompanyId,LevelCode,LevelName,RankOrder,IsActive)
                     SELECT ?,?,?,?,? WHERE EXISTS(SELECT 1 FROM Companies WHERE CompanyId=? AND IsActive=1)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1,level.getCompanyId()); statement.setString(2,level.getLevelCode());
            statement.setString(3,level.getLevelName()); statement.setInt(4,level.getRankOrder());
            statement.setBoolean(5,level.isActive()); statement.setInt(6,level.getCompanyId());
            if (statement.executeUpdate()!=1) return false;
            try (ResultSet keys=statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Chưa chọn cấp trách nhiệm."); level.setResponsibilityLevelId(keys.getInt(1)); }
            if (level.isAllowanceSpecified()) new OrganizationAllowanceDAO().setLevelAllowance(connection,level.getCompanyId(),level.getResponsibilityLevelId(),level.getDefaultAllowance());
            return true;
            }
        });
    }

    public boolean updateLevel(ResponsibilityLevel level) throws SQLException {
        return OrganizationTransaction.execute(level.getCompanyId(),connection->{
            try (PreparedStatement statement=connection.prepareStatement("""
                    UPDATE r SET LevelCode=?,LevelName=?,RankOrder=?,IsActive=?,UpdatedAt=SYSUTCDATETIME()
                    FROM ResponsibilityLevels r WITH (UPDLOCK,HOLDLOCK)
                    WHERE r.CompanyId=? AND r.ResponsibilityLevelId=?
                    AND ((r.LevelCode=? AND r.RankOrder=?) OR (
                        NOT EXISTS(SELECT 1 FROM Positions p WITH (UPDLOCK,HOLDLOCK) WHERE p.CompanyId=r.CompanyId AND p.ResponsibilityLevelId=r.ResponsibilityLevelId)
                        AND NOT EXISTS(SELECT 1 FROM ResponsibilityAllowancePolicies a WITH (UPDLOCK,HOLDLOCK) WHERE a.CompanyId=r.CompanyId AND a.ResponsibilityLevelId=r.ResponsibilityLevelId)))
                    """)) {
                statement.setString(1,level.getLevelCode()); statement.setString(2,level.getLevelName());
                statement.setInt(3,level.getRankOrder()); statement.setBoolean(4,level.isActive());
                statement.setInt(5,level.getCompanyId()); statement.setInt(6,level.getResponsibilityLevelId());
                statement.setString(7,level.getLevelCode()); statement.setInt(8,level.getRankOrder());
                if (statement.executeUpdate()!=1) return false;
                if (level.isAllowanceSpecified()) new OrganizationAllowanceDAO().setLevelAllowance(connection,level.getCompanyId(),level.getResponsibilityLevelId(),level.getDefaultAllowance());
                return true;
            }
        });
    }

    public boolean deactivateLevel(int companyId,int levelId) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            try (PreparedStatement statement=connection.prepareStatement("UPDATE ResponsibilityLevels SET IsActive=0,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND ResponsibilityLevelId=?")) {
                statement.setInt(1,companyId); statement.setInt(2,levelId);
                return statement.executeUpdate()==1;
            }
        });
    }

    private ResponsibilityLevel map(ResultSet rows) throws SQLException {
        ResponsibilityLevel level=new ResponsibilityLevel();
        level.setResponsibilityLevelId(rows.getInt("ResponsibilityLevelId"));
        level.setCompanyId(rows.getInt("CompanyId")); level.setLevelCode(rows.getString("LevelCode"));
        level.setLevelName(rows.getString("LevelName")); level.setRankOrder(rows.getInt("RankOrder"));
        level.setActive(rows.getBoolean("IsActive"));
        level.setDefaultAllowance(rows.getBigDecimal("DefaultAllowance"));
        level.clearAllowanceSpecified();
        Timestamp created=rows.getTimestamp("CreatedAt"),updated=rows.getTimestamp("UpdatedAt");
        level.setCreatedAt(created==null?null:created.toLocalDateTime());
        level.setUpdatedAt(updated==null?null:updated.toLocalDateTime());
        return level;
    }
}
