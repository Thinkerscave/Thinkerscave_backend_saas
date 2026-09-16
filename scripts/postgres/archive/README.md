# Historical SQL — not used by the application

Former Flyway-style schema scripts moved out of `src/main/resources/db`
so Spring never executes them on startup.

For platform master data, use `../01_platform_master_seed.sql` only.
