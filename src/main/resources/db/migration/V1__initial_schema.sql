CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(100),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    language VARCHAR(10) NOT NULL DEFAULT 'uz',
    bot_state VARCHAR(50) NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    total_pages INTEGER NOT NULL CHECK (total_pages > 0),
    pdf_file_path VARCHAR(512),
    pdf_file_name VARCHAR(255),
    cover_image_path VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reading_progresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    current_page INTEGER NOT NULL,
    progress_percent NUMERIC(5,2) NOT NULL,
    note VARCHAR(1000),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reading_progress_user_book UNIQUE (user_id, book_id)
);

CREATE TABLE book_ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    rating_value INTEGER NOT NULL CHECK (rating_value BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_book_rating_user_book UNIQUE (user_id, book_id)
);
