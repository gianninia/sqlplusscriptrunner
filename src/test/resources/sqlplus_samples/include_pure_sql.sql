-- an insert
INSERT INTO A_TABLE (ID, A_FIELD) VALUES (1, 'A_VALUE');

-- an update
UPDATE A_TABLE SET A_FIELD = 'A_VALUE';

-- a PL/SQL block
DECLARE
  my_var INT;
BEGIN
  my_var := my_function('TEST');
  COMMIT;
END;
/