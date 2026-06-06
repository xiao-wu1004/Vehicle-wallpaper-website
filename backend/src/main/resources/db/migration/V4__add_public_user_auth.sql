create table user_accounts (
    id bigint not null auto_increment,
    public_key varchar(96) not null,
    email varchar(160) not null,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    active boolean not null,
    last_login_at timestamp null,
    last_login_ip varchar(64) null,
    last_login_user_agent varchar(512) null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint pk_user_accounts primary key (id),
    constraint uk_user_accounts_public_key unique (public_key),
    constraint uk_user_accounts_email unique (email)
);

create table user_sessions (
    id bigint not null auto_increment,
    account_id bigint not null,
    token_hash varchar(64) not null,
    issued_at timestamp not null,
    last_seen_at timestamp not null,
    expires_at timestamp not null,
    revoked_at timestamp null,
    issued_ip varchar(64) not null,
    issued_user_agent varchar(512) not null,
    constraint pk_user_sessions primary key (id),
    constraint fk_user_sessions_account foreign key (account_id) references user_accounts (id),
    constraint uk_user_sessions_token_hash unique (token_hash)
);

create index idx_user_sessions_account_active on user_sessions (account_id, revoked_at, expires_at);
