create table brands (
    id bigint not null auto_increment,
    slug varchar(64) not null,
    display_name varchar(128) not null,
    folder_name varchar(128) not null,
    sort_order int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint pk_brands primary key (id),
    constraint uk_brands_slug unique (slug)
);

create table wallpapers (
    id bigint not null auto_increment,
    brand_id bigint not null,
    slug varchar(128) not null,
    title varchar(255) not null,
    file_name varchar(255) not null,
    preview_url varchar(512) not null,
    full_url varchar(512) not null,
    download_url varchar(512) not null,
    sort_order int not null,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint pk_wallpapers primary key (id),
    constraint fk_wallpapers_brand foreign key (brand_id) references brands (id),
    constraint uk_wallpapers_slug unique (slug),
    constraint uk_wallpapers_brand_file unique (brand_id, file_name)
);

create index idx_wallpapers_brand_sort on wallpapers (brand_id, sort_order);

create table feedback_messages (
    id bigint not null auto_increment,
    name varchar(80) not null,
    email varchar(160) not null,
    message text not null,
    status varchar(16) not null,
    featured boolean not null,
    source_page varchar(255) not null,
    user_agent varchar(512) not null,
    created_at timestamp not null,
    constraint pk_feedback_messages primary key (id)
);

create index idx_feedback_status_featured_created on feedback_messages (status, featured, created_at);
