create table wallpaper_metrics (
    wallpaper_id bigint not null,
    favorite_count bigint not null default 0,
    download_count bigint not null default 0,
    updated_at timestamp not null,
    constraint pk_wallpaper_metrics primary key (wallpaper_id),
    constraint fk_wallpaper_metrics_wallpaper foreign key (wallpaper_id) references wallpapers (id)
);

insert into wallpaper_metrics (wallpaper_id, favorite_count, download_count, updated_at)
select wallpapers.id,
       coalesce(favorite_totals.favorite_count, 0),
       coalesce(download_totals.download_count, 0),
       current_timestamp
from wallpapers
left join (
    select wallpaper_id, count(*) as favorite_count
    from wallpaper_favorites
    group by wallpaper_id
) favorite_totals on favorite_totals.wallpaper_id = wallpapers.id
left join (
    select wallpaper_id, count(*) as download_count
    from wallpaper_download_events
    group by wallpaper_id
) download_totals on download_totals.wallpaper_id = wallpapers.id;
