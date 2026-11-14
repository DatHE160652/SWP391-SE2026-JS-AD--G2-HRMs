-- Remove the legacy database-backed FAQ objects.
-- Safe to run more than once: each object is checked before removal.

IF OBJECT_ID(N'dbo.FaqComments', N'U') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = N'IX_FaqComments_Topic_CreatedAt'
         AND object_id = OBJECT_ID(N'dbo.FaqComments', N'U')
   )
BEGIN
    DROP INDEX [IX_FaqComments_Topic_CreatedAt] ON [dbo].[FaqComments];
END;
GO

IF OBJECT_ID(N'dbo.FaqComments', N'U') IS NOT NULL
BEGIN
    DROP TABLE [dbo].[FaqComments];
END;
GO

IF OBJECT_ID(N'dbo.FaqTopics', N'U') IS NOT NULL
BEGIN
    DROP TABLE [dbo].[FaqTopics];
END;
GO
