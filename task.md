Entwickle eine Anwendung in einer Sprache und Framework deiner Wahl, die folgende Dinge erledigt:

Herunterladen der TSL von http://download-testref.crl.ti-dienste.de/TSL-ECC-test/ECC-RSA_TSL-test.xml
Finden aller Eintraege vom Type TrustServiceProvider die mindestens einen ServiceTypeIdentifier http://uri.etsi.org/TrstSvc/Svctype/unspecified  und ExtensionValue oid_tigw_zugm haben.
Schreibe die gefundenen Informationen in eine Datenbanktabelle mit folgender Struktur:


CREATE TABLE `ti_gateway_provider` (

  `ti_gateway_provider_id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,

  `name` varchar(255) NOT NULL DEFAULT '',

  `service_name` varchar(255) NOT NULL DEFAULT '',

  `config_endpoint` varchar(255) DEFAULT NULL COMMENT 'ServiceSupplyPoint',

  `active` tinyint(1) DEFAULT NULL COMMENT 'http://uri.etsi.org/TrstSvc/Svcstatus/inaccord',

  `certificate` blob DEFAULT NULL,

  `issuer` varchar(255) DEFAULT NULL COMMENT 'ServiceSupplyPoint',

  `authorization_endpoint` varchar(255) DEFAULT NULL,

  `token_endpoint` varchar(255) DEFAULT NULL,

  `jwks_uri` varchar(255) DEFAULT NULL,

  `response_types_supported` varchar(255) DEFAULT NULL,

  `created_date` timestamp(6) NOT NULL DEFAULT current_timestamp(6),

  `updated_date` timestamp(6) NULL DEFAULT NULL ON UPDATE current_timestamp(6),

  `version` bigint(20) NOT NULL,

  PRIMARY KEY (`ti_gateway_provider_id`),

  UNIQUE KEY `service_name_idx` (`service_name`),

  KEY `active_idx` (`active`),

  KEY `version_idx` (`version`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

 

Die Werte fuer die Felder authorization_endpoint, token_endpoint, jwks_uri und response_types_supported werden durch Aufruf von <config_endpoint>/.well-known/openid-configuration ermittelt. Benutze das <certificate> in einem TrustStore fuer die verbindung zum <config_endpoint> (einseitige TLS-Verbindung)
Fuehre den Task periodisch per Scheduler o.ae. aus, sodass bestehende Datensaetze geupdated, neue hinzugefuegt und nicht mehr vorhandene geloescht werden.
Schreibe angemessene Tests um das Verhalten deiner Anwendung zu dokumentieren und fuer zukuenftige Aenderungen robust zu halten.
