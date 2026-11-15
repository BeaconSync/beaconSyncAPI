-- Uma sessão por login/aparelho; datas UTC. Não guarda o refresh token em texto.
CREATE TABLE BS_SESSAO_REFRESH (
                                   id           VARCHAR2(36 CHAR) NOT NULL,
                                   usuario_id   NUMBER(19) NOT NULL,
                                   criada_em    TIMESTAMP(6) NOT NULL,
                                   expira_em    TIMESTAMP(6) NOT NULL,
                                   revogada_em  TIMESTAMP(6),
                                   CONSTRAINT PK_BS_SESSAO_REFRESH PRIMARY KEY (id),
                                   CONSTRAINT FK_BS_REFRESH_USUARIO FOREIGN KEY (usuario_id) REFERENCES BS_USUARIO(id) ON DELETE CASCADE
);
CREATE INDEX IX_BS_REFRESH_USUARIO ON BS_SESSAO_REFRESH(usuario_id);
CREATE INDEX IX_BS_REFRESH_EXPIRA ON BS_SESSAO_REFRESH(expira_em);

CREATE TABLE BS_REFRESH_TOKEN (
                                  token_hash  VARCHAR2(64 CHAR) NOT NULL,
                                  sessao_id   VARCHAR2(36 CHAR) NOT NULL,
                                  criado_em   TIMESTAMP(6) NOT NULL,
                                  usado_em    TIMESTAMP(6),
                                  CONSTRAINT PK_BS_REFRESH_TOKEN PRIMARY KEY (token_hash),
                                  CONSTRAINT FK_BS_REFRESH_SESSAO FOREIGN KEY (sessao_id) REFERENCES BS_SESSAO_REFRESH(id) ON DELETE CASCADE
);
CREATE INDEX IX_BS_REFRESH_SESSAO ON BS_REFRESH_TOKEN(sessao_id);