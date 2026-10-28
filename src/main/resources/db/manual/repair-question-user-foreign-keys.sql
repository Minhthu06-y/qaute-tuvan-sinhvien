/*
 * Repair question/answer relationships left pointing at the legacy account table.
 * This migration changes constraints only; it does not update or delete rows.
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
    THROW 51100, 'dbo.users is required before repairing question user foreign keys.', 1;
IF OBJECT_ID(N'dbo.questions', N'U') IS NULL
    THROW 51101, 'dbo.questions is required before repairing question user foreign keys.', 1;
IF OBJECT_ID(N'dbo.answers', N'U') IS NULL
    THROW 51102, 'dbo.answers is required before repairing question user foreign keys.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (
        SELECT 1 FROM dbo.questions AS q
        LEFT JOIN dbo.users AS u ON u.id = q.asker_id
        WHERE q.asker_id IS NOT NULL AND u.id IS NULL
    )
        THROW 51103, 'Some questions.asker_id values do not exist in dbo.users; no constraints were changed.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.answers AS a
        LEFT JOIN dbo.users AS u ON u.id = a.author_id
        WHERE a.author_id IS NOT NULL AND u.id IS NULL
    )
        THROW 51104, 'Some answers.author_id values do not exist in dbo.users; no constraints were changed.', 1;

    IF OBJECT_ID(N'dbo.question_transfers', N'U') IS NOT NULL
       AND EXISTS (
           SELECT 1 FROM dbo.question_transfers AS t
           LEFT JOIN dbo.users AS u ON u.id = t.transferred_by_id
           WHERE t.transferred_by_id IS NOT NULL AND u.id IS NULL
       )
        THROW 51105, 'Some question_transfers.transferred_by_id values do not exist in dbo.users; no constraints were changed.', 1;

    DECLARE @dropWrongForeignKeys nvarchar(max) = N'';
    SELECT @dropWrongForeignKeys = @dropWrongForeignKeys
        + N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(fk.parent_object_id))
        + N'.' + QUOTENAME(OBJECT_NAME(fk.parent_object_id))
        + N' DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys AS fk
    INNER JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
    INNER JOIN sys.columns AS parent_column
        ON parent_column.object_id = fkc.parent_object_id
       AND parent_column.column_id = fkc.parent_column_id
    WHERE fk.referenced_object_id <> OBJECT_ID(N'dbo.users')
      AND (
          (fk.parent_object_id = OBJECT_ID(N'dbo.questions') AND parent_column.name = N'asker_id')
          OR (fk.parent_object_id = OBJECT_ID(N'dbo.answers') AND parent_column.name = N'author_id')
          OR (fk.parent_object_id = OBJECT_ID(N'dbo.question_transfers') AND parent_column.name = N'transferred_by_id')
      );

    IF @dropWrongForeignKeys <> N''
        EXEC sys.sp_executesql @dropWrongForeignKeys;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys AS fk
        INNER JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
        INNER JOIN sys.columns AS parent_column
            ON parent_column.object_id = fkc.parent_object_id
           AND parent_column.column_id = fkc.parent_column_id
        INNER JOIN sys.columns AS referenced_column
            ON referenced_column.object_id = fkc.referenced_object_id
           AND referenced_column.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N'dbo.questions')
          AND fk.referenced_object_id = OBJECT_ID(N'dbo.users')
          AND parent_column.name = N'asker_id'
          AND referenced_column.name = N'id'
    )
        ALTER TABLE dbo.questions WITH CHECK ADD CONSTRAINT FK_questions_users_asker
            FOREIGN KEY (asker_id) REFERENCES dbo.users(id);

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys AS fk
        INNER JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
        INNER JOIN sys.columns AS parent_column
            ON parent_column.object_id = fkc.parent_object_id
           AND parent_column.column_id = fkc.parent_column_id
        INNER JOIN sys.columns AS referenced_column
            ON referenced_column.object_id = fkc.referenced_object_id
           AND referenced_column.column_id = fkc.referenced_column_id
        WHERE fk.parent_object_id = OBJECT_ID(N'dbo.answers')
          AND fk.referenced_object_id = OBJECT_ID(N'dbo.users')
          AND parent_column.name = N'author_id'
          AND referenced_column.name = N'id'
    )
        ALTER TABLE dbo.answers WITH CHECK ADD CONSTRAINT FK_answers_users_author
            FOREIGN KEY (author_id) REFERENCES dbo.users(id);

    IF OBJECT_ID(N'dbo.question_transfers', N'U') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM sys.foreign_keys AS fk
           INNER JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
           INNER JOIN sys.columns AS parent_column
               ON parent_column.object_id = fkc.parent_object_id
              AND parent_column.column_id = fkc.parent_column_id
           INNER JOIN sys.columns AS referenced_column
               ON referenced_column.object_id = fkc.referenced_object_id
              AND referenced_column.column_id = fkc.referenced_column_id
           WHERE fk.parent_object_id = OBJECT_ID(N'dbo.question_transfers')
             AND fk.referenced_object_id = OBJECT_ID(N'dbo.users')
             AND parent_column.name = N'transferred_by_id'
             AND referenced_column.name = N'id'
       )
        ALTER TABLE dbo.question_transfers WITH CHECK ADD CONSTRAINT FK_question_transfers_users_transferred_by
            FOREIGN KEY (transferred_by_id) REFERENCES dbo.users(id);

    COMMIT TRANSACTION;
    PRINT 'Question and answer user foreign keys now reference dbo.users.';

    SELECT fk.name,
           OBJECT_NAME(fk.parent_object_id) AS parent_table,
           parent_column.name AS parent_column,
           OBJECT_NAME(fk.referenced_object_id) AS referenced_table
    FROM sys.foreign_keys AS fk
    INNER JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
    INNER JOIN sys.columns AS parent_column
        ON parent_column.object_id = fkc.parent_object_id
       AND parent_column.column_id = fkc.parent_column_id
    WHERE fk.referenced_object_id = OBJECT_ID(N'dbo.users')
      AND ((fk.parent_object_id = OBJECT_ID(N'dbo.questions') AND parent_column.name = N'asker_id')
        OR (fk.parent_object_id = OBJECT_ID(N'dbo.answers') AND parent_column.name = N'author_id')
        OR (fk.parent_object_id = OBJECT_ID(N'dbo.question_transfers') AND parent_column.name = N'transferred_by_id'));
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
