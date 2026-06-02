alter table wallpapers
    add column storage_provider varchar(32) not null default 'filesystem';

alter table wallpapers
    add column storage_key varchar(512);

alter table wallpapers
    add column preview_storage_key varchar(512);

create table wallpaper_favorites (
    id bigint not null auto_increment,
    wallpaper_id bigint not null,
    visitor_key varchar(96) not null,
    created_at timestamp not null,
    constraint pk_wallpaper_favorites primary key (id),
    constraint fk_wallpaper_favorites_wallpaper foreign key (wallpaper_id) references wallpapers (id),
    constraint uk_wallpaper_favorites_visitor_wallpaper unique (visitor_key, wallpaper_id)
);

create index idx_wallpaper_favorites_wallpaper on wallpaper_favorites (wallpaper_id);
create index idx_wallpaper_favorites_visitor_created on wallpaper_favorites (visitor_key, created_at);

create table wallpaper_download_events (
    id bigint not null auto_increment,
    wallpaper_id bigint not null,
    visitor_key varchar(96) not null,
    created_at timestamp not null,
    constraint pk_wallpaper_download_events primary key (id),
    constraint fk_wallpaper_download_events_wallpaper foreign key (wallpaper_id) references wallpapers (id)
);

create index idx_wallpaper_download_events_wallpaper on wallpaper_download_events (wallpaper_id);
create index idx_wallpaper_download_events_visitor_created on wallpaper_download_events (visitor_key, created_at);
