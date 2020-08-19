-- Here are all table definitions used by LibertyBans
-- These queries are also dynamically shaped by the database vendor.
-- Some data type arguments are also defined at runtime.

-- The database revision table, which contains the unique "constant" column
-- with only one possible value to ensure there is only one row

CREATE TABLE `libertybans_revision` (
`constant` ENUM('Constant') NOT NULL UNIQUE, 
`major` INT NOT NULL, 
`minor` INT NOT NULL);

-- The reason these are not the same table is that some servers
-- may need to periodically clear the addresses table per GDPR
-- regulations. However, using a single table with a null address
-- would be unwise, since unique constraints don't work nicely
-- with null values.

CREATE TABLE `libertybans_names` (
`uuid` BINARY(16) NOT NULL, 
`name` VARCHAR(16) NOT NULL, 
`updated` INT UNSIGNED NOT NULL, 
PRIMARY KEY (`uuid`), 
UNIQUE (`uuid`, `name`));

CREATE TABLE `libertybans_addresses` (
`uuid` BINARY(16) NOT NULL, 
`address` VARBINARY(16) NOT NULL, 
`updated` INT UNSIGNED NOT NULL, 
PRIMARY KEY (`uuid`), 
UNIQUE (`uuid`, `address`));

-- Primary punishments table from which others are derived

CREATE TABLE `libertybans_punishments` (
`id` INT AUTO_INCREMENT PRIMARY KEY, 
`type` <punishmentTypeInfo>, 
`operator` BINARY(16) NOT NULL, 
`reason` VARCHAR(256) NOT NULL, 
`scope` VARCHAR(32) NULL DEFAULT NULL, 
`start` INT UNSIGNED NOT NULL, 
`end` INT UNSIGNED NOT NULL);

-- Individual punishment tables
-- These are separate so that they may have different constraints

CREATE TABLE `libertybans_bans` (
`id` INT PRIMARY KEY, 
`victim` <victimInfo>, 
`victim_type` <victimTypeInfo>, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, 
UNIQUE (`victim`, `victim_type`));

CREATE TABLE `libertybans_mutes` (
`id` INT PRIMARY KEY, 
`victim` <victimInfo>, 
`victim_type` <victimTypeInfo>, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE, 
UNIQUE (`victim`, `victim_type`));

CREATE TABLE `libertybans_warns` (
`id` INT PRIMARY KEY, 
`victim` <victimInfo>, 
`victim_type` <victimTypeInfo>, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE);

CREATE TABLE `libertybans_history` (
`id` INT NOT NULL, 
`victim` <victimInfo>, 
`victim_type` <victimTypeInfo>, 
FOREIGN KEY (`id`) REFERENCES `libertybans_punishments` (`id`) ON DELETE CASCADE);
