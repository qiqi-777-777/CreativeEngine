DROP VIEW IF EXISTS `v_policy_title_list`;
CREATE VIEW `v_policy_title_list` AS
SELECT `id`,`title` FROM `policy_item`;

DROP PROCEDURE IF EXISTS `sp_get_policy_by_title`;
DELIMITER $$
CREATE PROCEDURE `sp_get_policy_by_title`(IN p_title VARCHAR(255))
BEGIN
  SELECT `id`,`title`,`keywords`,`content`
  FROM `policy_item`
  WHERE `title` = p_title
  LIMIT 1;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `sp_search_policy_by_title`;
DELIMITER $$
CREATE PROCEDURE `sp_search_policy_by_title`(IN p_q VARCHAR(255))
BEGIN
  SELECT `id`,`title`
  FROM `policy_item`
  WHERE `title` LIKE CONCAT('%', p_q, '%')
  ORDER BY `id` DESC;
END$$
DELIMITER ;