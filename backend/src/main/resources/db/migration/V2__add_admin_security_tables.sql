create table admin_accounts (
    id bigint not null auto_increment,
    username varchar(80) not null,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    active boolean not null,
    failed_login_attempts int not null,
    locked_until timestamp null,
    session_version bigint not null,
    last_login_at timestamp null,
    last_login_ip varchar(64) null,
    last_login_user_agent varchar(512) null,
    password_updated_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint pk_admin_accounts primary key (id),
    constraint uk_admin_accounts_username unique (username)
);

create table admin_sessions (
    id bigint not null auto_increment,
    account_id bigint not null,
    token_hash varchar(128) not null,
    auth_mode varchar(32) not null,
    session_version bigint not null,
    issued_at timestamp not null,
    expires_at timestamp not null,
    revoked_at timestamp null,
    last_seen_at timestamp not null,
    issued_ip varchar(64) not null,
    issued_user_agent varchar(512) not null,
    constraint pk_admin_sessions primary key (id),
    constraint fk_admin_sessions_account foreign key (account_id) references admin_accounts (id),
    constraint uk_admin_sessions_token_hash unique (token_hash)
);

create index idx_admin_sessions_account_active on admin_sessions (account_id, revoked_at, expires_at);

create table admin_operation_logs (
    id bigint not null auto_increment,
    account_id bigint null,
    actor_username varchar(80) not null,
    auth_mode varchar(32) not null,
    action varchar(80) not null,
    target_type varchar(80) not null,
    target_id varchar(120) null,
    detail text null,
    request_path varchar(255) not null,
    ip_address varchar(64) not null,
    user_agent varchar(512) not null,
    created_at timestamp not null,
    constraint pk_admin_operation_logs primary key (id),
    constraint fk_admin_operation_logs_account foreign key (account_id) references admin_accounts (id)
);

create index idx_admin_operation_logs_created on admin_operation_logs (created_at);
