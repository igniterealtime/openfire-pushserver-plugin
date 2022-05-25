CREATE TABLE ofPushServer (
    domain      VARCHAR(64)             NOT NULL,
    deviceId    VARCHAR(64)             NOT NULL,
    token       VARCHAR(1024)           NULL,
    node        VARCHAR(1024)           NULL,
    secret      VARCHAR(1024)           NULL,
    type        VARCHAR(16)             NOT NULL,
    CONSTRAINT ofPushServer_pk PRIMARY KEY (domain, deviceId)
);

INSERT INTO ofVersion (name, version) VALUES ('pushserver', 1);