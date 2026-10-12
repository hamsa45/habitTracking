create table if not exists LearningPlans (
    id integer primary key autoincrement,
    name text not null,
    start_date text not null,
    current_day_index integer not null default 1,
    status text not null default 'active',
    created_at integer not null
);

create table if not exists LearningDays (
    id integer primary key autoincrement,
    plan_id integer not null,
    day_index integer not null,
    title text not null,
    description text not null default '',
    estimated_minutes integer not null default 45,
    is_rest_day integer not null default 0,
    status text not null default 'pending',
    scheduled_date text,
    foreign key(plan_id) references LearningPlans(id) on delete cascade
);

create table if not exists LearningTasks (
    id integer primary key autoincrement,
    day_id integer not null,
    position integer not null default 0,
    title text not null,
    description text not null default '',
    deliverable text not null default '',
    is_done integer not null default 0,
    foreign key(day_id) references LearningDays(id) on delete cascade
);

create table if not exists StudySessions (
    id integer primary key autoincrement,
    uuid text not null,
    started_at integer not null,
    ended_at integer not null,
    duration_ms integer not null,
    paused_ms integer not null default 0,
    timing_source text not null default 'timer',
    objective text not null default '',
    learned text not null default '',
    attempted text not null default '',
    unclear text not null default '',
    next_action text not null default '',
    plan_day_id integer,
    topic_id integer,
    calendar_date text not null
);

create unique index if not exists idx_study_sessions_uuid on StudySessions(uuid);
create index if not exists idx_study_sessions_date on StudySessions(calendar_date);

create table if not exists ActiveStudySession (
    id integer primary key check (id = 1),
    started_elapsed_realtime integer not null,
    accumulated_ms integer not null default 0,
    pause_started_elapsed integer,
    state text not null,
    objective text not null default '',
    day_id integer,
    topic_id integer,
    wall_started_at integer not null
);

create table if not exists LearningTopics (
    id integer primary key autoincrement,
    name text not null,
    category text not null default '',
    status text not null default 'to_learn',
    first_studied_at integer,
    last_reviewed_at integer,
    next_review_at integer,
    notes text not null default '',
    mastery_flags integer not null default 0,
    rating integer not null default 0
);

create table if not exists RevisionTasks (
    id integer primary key autoincrement,
    topic_id integer not null,
    interval_days integer not null,
    due_date text not null,
    status text not null default 'pending',
    foreign key(topic_id) references LearningTopics(id) on delete cascade
);

create unique index if not exists idx_revision_pending
    on RevisionTasks(topic_id, interval_days)
    where status = 'pending';

create table if not exists WeeklyReviews (
    id integer primary key autoincrement,
    week_start_date text not null,
    completed_summary text not null default '',
    minutes_reflection text not null default '',
    explain_implement text not null default '',
    needs_revision text not null default '',
    distractions text not null default '',
    next_priority text not null default '',
    adjustment text not null default '',
    created_at integer not null,
    updated_at integer not null
);

create unique index if not exists idx_weekly_reviews_week on WeeklyReviews(week_start_date);
