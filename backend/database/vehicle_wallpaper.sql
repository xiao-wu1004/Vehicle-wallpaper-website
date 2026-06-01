-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: vehicle_wallpaper
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `brands`
--

DROP TABLE IF EXISTS `brands`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brands` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slug` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `folder_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `created_at` timestamp NOT NULL,
  `updated_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brands_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `brands`
--

LOCK TABLES `brands` WRITE;
/*!40000 ALTER TABLE `brands` DISABLE KEYS */;
INSERT INTO `brands` VALUES (1,'benz','奔驰','MercedesBenz',1,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(2,'porsche','保时捷','Porsche',2,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(3,'hongqi','红旗','HongQi',3,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(4,'xiaomi','小米','Xiaomi',4,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(5,'bmw','宝马','BMW',5,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(6,'audi','奥迪','Audi',6,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(7,'ferrari','法拉利','Ferrari',7,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(8,'lamborghini','兰博基尼','Lamborghini',8,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(9,'astonmartin','阿斯顿马丁','Aston Martin',9,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(10,'maserati','玛莎拉蒂','Maserati',10,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(11,'bugatti','布加迪','Bugatti',11,'2026-06-01 04:48:36','2026-06-01 04:48:36'),(12,'ford','福特','Ford',12,'2026-06-01 04:48:36','2026-06-01 04:48:36');
/*!40000 ALTER TABLE `brands` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `feedback_messages`
--

DROP TABLE IF EXISTS `feedback_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `featured` tinyint(1) NOT NULL,
  `source_page` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status_featured_created` (`status`,`featured`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedback_messages`
--

LOCK TABLES `feedback_messages` WRITE;
/*!40000 ALTER TABLE `feedback_messages` DISABLE KEYS */;
INSERT INTO `feedback_messages` VALUES (1,'用户A','user.a@example.com','在这里找到的壁纸质量非常高，让我每次打开电脑都很愉快！','APPROVED',1,'/seed','system-seed','2026-06-01 04:48:36'),(2,'用户B','user.b@example.com','我很喜欢这个网站的设计风格，壁纸种类丰富，下载体验也很流畅。','APPROVED',1,'/seed','system-seed','2026-06-01 04:48:36');
/*!40000 ALTER TABLE `feedback_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','create core tables','SQL','V1__create_core_tables.sql',-1741592629,'root','2026-06-01 04:48:34',126,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallpapers`
--

DROP TABLE IF EXISTS `wallpapers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallpapers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `brand_id` bigint NOT NULL,
  `slug` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `preview_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `download_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `active` tinyint(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  `updated_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallpapers_slug` (`slug`),
  UNIQUE KEY `uk_wallpapers_brand_file` (`brand_id`,`file_name`),
  KEY `idx_wallpapers_brand_sort` (`brand_id`,`sort_order`),
  CONSTRAINT `fk_wallpapers_brand` FOREIGN KEY (`brand_id`) REFERENCES `brands` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallpapers`
--

LOCK TABLES `wallpapers` WRITE;
/*!40000 ALTER TABLE `wallpapers` DISABLE KEYS */;
INSERT INTO `wallpapers` VALUES (106,1,'benz-1','奔驰壁纸1','MercedesBenz.jpg','/cars/_thumb/MercedesBenz/MercedesBenz.webp','/cars/MercedesBenz/MercedesBenz.jpg','/cars/MercedesBenz/MercedesBenz.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(107,1,'benz-2','奔驰壁纸2','MercedesBenz(2).jpg','/cars/_thumb/MercedesBenz/MercedesBenz(2).webp','/cars/MercedesBenz/MercedesBenz(2).jpg','/cars/MercedesBenz/MercedesBenz(2).jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(108,1,'benz-3','奔驰壁纸3','mercedes-amg-gt3-2024-3840x2160-18212.jpg','/cars/_thumb/MercedesBenz/mercedes-amg-gt3-2024-3840x2160-18212.webp','/cars/MercedesBenz/mercedes-amg-gt3-2024-3840x2160-18212.jpg','/cars/MercedesBenz/mercedes-amg-gt3-2024-3840x2160-18212.jpg',3,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(109,1,'benz-4','奔驰壁纸4','mercedes-benz-amg-gt-roadster-black-cars-edo-competition-3840x2160-6747.jpg','/cars/_thumb/MercedesBenz/mercedes-benz-amg-gt-roadster-black-cars-edo-competition-3840x2160-6747.webp','/cars/MercedesBenz/mercedes-benz-amg-gt-roadster-black-cars-edo-competition-3840x2160-6747.jpg','/cars/MercedesBenz/mercedes-benz-amg-gt-roadster-black-cars-edo-competition-3840x2160-6747.jpg',4,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(110,2,'porsche-1','保时捷壁纸1','Porsche.jpeg','/cars/_thumb/Porsche/Porsche.webp','/cars/Porsche/Porsche.jpeg','/cars/Porsche/Porsche.jpeg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(111,2,'porsche-2','保时捷壁纸2','Porsche(1).jpg','/cars/_thumb/Porsche/Porsche(1).webp','/cars/Porsche/Porsche(1).jpg','/cars/Porsche/Porsche(1).jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(112,2,'porsche-3','保时捷壁纸3','porsche-911-gt3-rs-3840x2160-18809.jpg','/cars/_thumb/Porsche/porsche-911-gt3-rs-3840x2160-18809.webp','/cars/Porsche/porsche-911-gt3-rs-3840x2160-18809.jpg','/cars/Porsche/porsche-911-gt3-rs-3840x2160-18809.jpg',3,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(113,2,'porsche-4','保时捷壁纸4','porsche-taycan-3840x2160-18805.jpg','/cars/_thumb/Porsche/porsche-taycan-3840x2160-18805.webp','/cars/Porsche/porsche-taycan-3840x2160-18805.jpg','/cars/Porsche/porsche-taycan-3840x2160-18805.jpg',4,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(114,2,'porsche-5','保时捷壁纸5','sportec-porsche-992-3840x2160-20098.jpg','/cars/_thumb/Porsche/sportec-porsche-992-3840x2160-20098.webp','/cars/Porsche/sportec-porsche-992-3840x2160-20098.jpg','/cars/Porsche/sportec-porsche-992-3840x2160-20098.jpg',5,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(115,3,'hongqi-1','红旗壁纸1','HongQi.jpeg','/cars/_thumb/HongQi/HongQi.webp','/cars/HongQi/HongQi.jpeg','/cars/HongQi/HongQi.jpeg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(116,3,'hongqi-2','红旗壁纸2','Hongqi_1920x1200.jpg','/cars/_thumb/HongQi/Hongqi_1920x1200.webp','/cars/HongQi/Hongqi_1920x1200.jpg','/cars/HongQi/Hongqi_1920x1200.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(117,4,'xiaomi-1','小米壁纸1','《小米SU7Ultra》4K_3840x2160.jpg','/cars/_thumb/Xiaomi/《小米SU7Ultra》4K_3840x2160.webp','/cars/Xiaomi/《小米SU7Ultra》4K_3840x2160.jpg','/cars/Xiaomi/《小米SU7Ultra》4K_3840x2160.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(118,4,'xiaomi-2','小米壁纸2','小米SU7Ultra跑车3440x1440.jpg','/cars/_thumb/Xiaomi/小米SU7Ultra跑车3440x1440.webp','/cars/Xiaomi/小米SU7Ultra跑车3440x1440.jpg','/cars/Xiaomi/小米SU7Ultra跑车3440x1440.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(119,5,'bmw-1','宝马壁纸1','BMW.jpg','/cars/_thumb/BMW/BMW.webp','/cars/BMW/BMW.jpg','/cars/BMW/BMW.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(120,5,'bmw-2','宝马壁纸2','bmw-m4.jpg','/cars/_thumb/BMW/bmw-m4.webp','/cars/BMW/bmw-m4.jpg','/cars/BMW/bmw-m4.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(121,6,'audi-1','奥迪壁纸1','audi.jpg','/cars/_thumb/Audi/audi.webp','/cars/Audi/audi.jpg','/cars/Audi/audi.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(122,6,'audi-2','奥迪壁纸2','audi(2).jpg','/cars/_thumb/Audi/audi(2).webp','/cars/Audi/audi(2).jpg','/cars/Audi/audi(2).jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(123,6,'audi-3','奥迪壁纸3','audi-r8-lms-gt3-evo-ii-race-cars-2022-5k-3840x2160-6155.jpeg','/cars/_thumb/Audi/audi-r8-lms-gt3-evo-ii-race-cars-2022-5k-3840x2160-6155.webp','/cars/Audi/audi-r8-lms-gt3-evo-ii-race-cars-2022-5k-3840x2160-6155.jpeg','/cars/Audi/audi-r8-lms-gt3-evo-ii-race-cars-2022-5k-3840x2160-6155.jpeg',3,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(124,7,'ferrari-1','法拉利壁纸1','ferrari-12cilindri-3840x2160-19297.jpeg','/cars/_thumb/Ferrari/ferrari-12cilindri-3840x2160-19297.webp','/cars/Ferrari/ferrari-12cilindri-3840x2160-19297.jpeg','/cars/Ferrari/ferrari-12cilindri-3840x2160-19297.jpeg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(125,7,'ferrari-2','法拉利壁纸2','ferrari-f80-red-3840x2160-19313.jpg','/cars/_thumb/Ferrari/ferrari-f80-red-3840x2160-19313.webp','/cars/Ferrari/ferrari-f80-red-3840x2160-19313.jpg','/cars/Ferrari/ferrari-f80-red-3840x2160-19313.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(126,8,'lamborghini-1','兰博基尼壁纸1','Lamborghini(3).jpg','/cars/_thumb/Lamborghini/Lamborghini(3).webp','/cars/Lamborghini/Lamborghini(3).jpg','/cars/Lamborghini/Lamborghini(3).jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(127,8,'lamborghini-2','兰博基尼壁纸2','lamborghini-3840x2160-11264.jpeg','/cars/_thumb/Lamborghini/lamborghini-3840x2160-11264.webp','/cars/Lamborghini/lamborghini-3840x2160-11264.jpeg','/cars/Lamborghini/lamborghini-3840x2160-11264.jpeg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(128,8,'lamborghini-3','兰博基尼壁纸3','lamborghini-essenza-3840x2160-19501.jpg','/cars/_thumb/Lamborghini/lamborghini-essenza-3840x2160-19501.webp','/cars/Lamborghini/lamborghini-essenza-3840x2160-19501.jpg','/cars/Lamborghini/lamborghini-essenza-3840x2160-19501.jpg',3,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(129,8,'lamborghini-4','兰博基尼壁纸4','lamborghini-essenza-scv12-hypercars-2020-3840x2160-1986.jpeg','/cars/_thumb/Lamborghini/lamborghini-essenza-scv12-hypercars-2020-3840x2160-1986.webp','/cars/Lamborghini/lamborghini-essenza-scv12-hypercars-2020-3840x2160-1986.jpeg','/cars/Lamborghini/lamborghini-essenza-scv12-hypercars-2020-3840x2160-1986.jpeg',4,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(130,8,'lamborghini-5','兰博基尼壁纸5','lamborghini-hurac-n-3840x2160-18432.jpg','/cars/_thumb/Lamborghini/lamborghini-hurac-n-3840x2160-18432.webp','/cars/Lamborghini/lamborghini-hurac-n-3840x2160-18432.jpg','/cars/Lamborghini/lamborghini-hurac-n-3840x2160-18432.jpg',5,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(131,9,'astonmartin-1','阿斯顿马丁壁纸1','aston-martin-3840x2160-18505.jpg','/cars/_thumb/Aston Martin/aston-martin-3840x2160-18505.webp','/cars/Aston Martin/aston-martin-3840x2160-18505.jpg','/cars/Aston Martin/aston-martin-3840x2160-18505.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(132,9,'astonmartin-2','阿斯顿马丁壁纸2','aston-martin-3840x2160-19160.jpg','/cars/_thumb/Aston Martin/aston-martin-3840x2160-19160.webp','/cars/Aston Martin/aston-martin-3840x2160-19160.jpg','/cars/Aston Martin/aston-martin-3840x2160-19160.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(133,9,'astonmartin-3','阿斯顿马丁壁纸3','aston-martin-3840x2160-19866.jpg','/cars/_thumb/Aston Martin/aston-martin-3840x2160-19866.webp','/cars/Aston Martin/aston-martin-3840x2160-19866.jpg','/cars/Aston Martin/aston-martin-3840x2160-19866.jpg',3,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(134,9,'astonmartin-4','阿斯顿马丁壁纸4','aston-martin-dbs-3840x2160-20002.jpg','/cars/_thumb/Aston Martin/aston-martin-dbs-3840x2160-20002.webp','/cars/Aston Martin/aston-martin-dbs-3840x2160-20002.jpg','/cars/Aston Martin/aston-martin-dbs-3840x2160-20002.jpg',4,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(135,10,'maserati-1','玛莎拉蒂壁纸1','maserati-mc20-2024-5k-3840x2160-18438.jpg','/cars/_thumb/Maserati/maserati-mc20-2024-5k-3840x2160-18438.webp','/cars/Maserati/maserati-mc20-2024-5k-3840x2160-18438.jpg','/cars/Maserati/maserati-mc20-2024-5k-3840x2160-18438.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(136,10,'maserati-2','玛莎拉蒂壁纸2','maserati-mcxtrema-3840x2160-18812.jpg','/cars/_thumb/Maserati/maserati-mcxtrema-3840x2160-18812.webp','/cars/Maserati/maserati-mcxtrema-3840x2160-18812.jpg','/cars/Maserati/maserati-mcxtrema-3840x2160-18812.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(137,11,'bugatti-1','布加迪壁纸1','bugatti-bolide-3840x2160-18182.jpg','/cars/_thumb/Bugatti/bugatti-bolide-3840x2160-18182.webp','/cars/Bugatti/bugatti-bolide-3840x2160-18182.jpg','/cars/Bugatti/bugatti-bolide-3840x2160-18182.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(138,11,'bugatti-2','布加迪壁纸2','bugatti-chiron-pur-3840x2160-18367.jpg','/cars/_thumb/Bugatti/bugatti-chiron-pur-3840x2160-18367.webp','/cars/Bugatti/bugatti-chiron-pur-3840x2160-18367.jpg','/cars/Bugatti/bugatti-chiron-pur-3840x2160-18367.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(139,12,'ford-1','福特壁纸1','ford-mustang-gtd-3840x2160-17135.jpg','/cars/_thumb/Ford/ford-mustang-gtd-3840x2160-17135.webp','/cars/Ford/ford-mustang-gtd-3840x2160-17135.jpg','/cars/Ford/ford-mustang-gtd-3840x2160-17135.jpg',1,1,'2026-06-01 05:18:16','2026-06-01 05:18:16'),(140,12,'ford-2','福特壁纸2','red-ford-mustang-gt-5k-8k-3840x2160-17475.jpg','/cars/_thumb/Ford/red-ford-mustang-gt-5k-8k-3840x2160-17475.webp','/cars/Ford/red-ford-mustang-gt-5k-8k-3840x2160-17475.jpg','/cars/Ford/red-ford-mustang-gt-5k-8k-3840x2160-17475.jpg',2,1,'2026-06-01 05:18:16','2026-06-01 05:18:16');
/*!40000 ALTER TABLE `wallpapers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-01 13:44:17
