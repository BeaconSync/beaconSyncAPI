-- Adiciona foto privada sem modificar migrations anteriores ou apagar registros.
ALTER TABLE BS_USUARIO ADD foto_versao VARCHAR2(36 CHAR);
CREATE TABLE BS_USUARIO_FOTO (
                                 usuario_id NUMBER(19) NOT NULL,
                                 conteudo BLOB NOT NULL,
                                 CONSTRAINT PK_BS_USUARIO_FOTO PRIMARY KEY (usuario_id),
                                 CONSTRAINT FK_BS_FOTO_USUARIO FOREIGN KEY (usuario_id) REFERENCES BS_USUARIO(id) ON DELETE CASCADE
);