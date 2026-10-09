package com.hrms.controller;

import com.hrms.model.Position;
import com.hrms.service.PositionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PositionServlet extends HttpServlet {
    private final PositionService positionService = new PositionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/positions/detail".equals(request.getServletPath())) {
                Position position = positionService.getPositionDetail(parseRequiredInt(request.getParameter("id")));
                if (position == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Position not found."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(position) + "}");
                return;
            }

            List<Position> positions = positionService.getPositions();
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(positions) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean success;
            String message;
            if ("/positions/delete".equals(request.getServletPath())) {
                success = positionService.deletePosition(parseRequiredInt(request.getParameter("positionId")));
                message = success ? "Position deleted successfully." : "No position record was changed.";
            } else if ("/positions/update".equals(request.getServletPath())) {
                success = positionService.updatePosition(readPosition(request));
                message = success ? "Position updated successfully." : "No position record was changed.";
            } else {
                success = positionService.createPosition(readPosition(request));
                message = success ? "Position created successfully." : "No position record was changed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    private Position readPosition(HttpServletRequest request) {
        Position position = new Position();
        position.setPositionId(parseOptionalInt(request.getParameter("positionId")));
        position.setDepartmentId(parseOptionalInteger(request.getParameter("departmentId")));
        position.setPositionTitle(trimToNull(request.getParameter("positionTitle")));
        position.setDescription(trimToNull(request.getParameter("description")));
        return position;
    }

    private String toJsonArray(List<Position> positions) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJson(positions.get(i)));
        }
        return json.append("]").toString();
    }

    private String toJson(Position position) {
        return "{"
                + "\"positionId\":" + position.getPositionId() + ","
                + "\"companyId\":" + position.getCompanyId() + ","
                + "\"departmentId\":" + nullableInteger(position.getDepartmentId()) + ","
                + "\"departmentName\":" + quote(position.getDepartmentName()) + ","
                + "\"positionTitle\":" + quote(position.getPositionTitle()) + ","
                + "\"description\":" + quote(position.getDescription()) + ","
                + "\"createdAt\":" + quote(position.getCreatedAt() == null ? null : position.getCreatedAt().toString())
                + "}";
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required numeric value is missing.");
        }
        return Integer.parseInt(value);
    }

    private int parseOptionalInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private Integer parseOptionalInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.parseInt(value);
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
