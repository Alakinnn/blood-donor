PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE android_metadata (locale TEXT);
INSERT INTO android_metadata VALUES('en_US');
CREATE TABLE users(id TEXT PRIMARY KEY,email TEXT UNIQUE NOT NULL,password TEXT NOT NULL,full_name TEXT NOT NULL,date_of_birth INTEGER NOT NULL,phone_number TEXT,user_type TEXT NOT NULL,blood_type TEXT,gender TEXT,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);
INSERT INTO users VALUES('87efefbf-72da-4e9c-984e-cbdf7c31c366','admin@blooddonor.com','pf+grEGbz3yZUhyRB94TGt16eUL3LnHtNn2xABgak1+hFcVPbWC4MRppGf+wqq3V','System Administrator',440679668078,NULL,'SUPER_USER',NULL,NULL,1734606068078,1734606068078);
INSERT INTO users VALUES('11f3ac31-0182-49c7-84f1-7421d0ea8ab4','donor1@test.com','PNmwSBhWYDXxOz+5OYVETKrcpqGuIjuGInEO0DA2+3+yAd3XF56asmWG7vIPDbMa','John Smith',945601268081,NULL,'DONOR','A+','M',1734606068082,1734606068082);
INSERT INTO users VALUES('70dcac99-212e-45f0-93d1-df7b755a6c62','donor2@test.com','UiZG5UAPtak0rqyx5fLlgehojBXh4Ar+9+Z7FSEsuHTw2jGhYc7xfk+RliF6gF8m','Sarah Johnson',693140468083,NULL,'DONOR','O-','F',1734606068083,1734606068083);
INSERT INTO users VALUES('0424e9de-d417-43c4-b081-e9a1c1fd4680','donor3@test.com','2M3ze3qfexjnV35DPyqrYVZ0RjXlCpM4wxUz3o2X+SCtcBIVsMaQRNoyFKZUpTaI','Michael Brown',-158849931916,NULL,'DONOR','B+','M',1734606068084,1734606068084);
INSERT INTO users VALUES('2592bc98-9a93-47fe-ac00-edd9d5d62d19','donor4@test.com','PTwHrYNOntrpQLX+HW9w1Ol27LqrgpPzXM6pr/ld/WBriexVqvCPl5+g055+No4J','Emily Davis',-190472331915,NULL,'DONOR','AB+','M',1734606068085,1734606068085);
INSERT INTO users VALUES('932c5c81-5218-4dae-b9e0-e47771d3cbe2','donor5@test.com','4E2fP1kdVjEel0x1V68yc8gOXQg6XfFqoF52HXQt6bLEOQaeGQc57NHQRYYvd0Rv','David Wilson',377607668086,NULL,'DONOR','O+','M',1734606068087,1734606068087);
INSERT INTO users VALUES('e4636529-52a7-4647-a080-d21850b88032','manager1@test.com','rq/0gZxEnvkAtqiyuu7KT4etoHaoyfNHL3qSwqHbaxltsQW9xCHCgvRvD04HXU5k','Robert Taylor',503838068090,'+1234567890','SITE_MANAGER',NULL,'F',1734606068090,1734606068090);
INSERT INTO users VALUES('6ba19497-b3b4-4cc5-be17-222457454d09','manager2@test.com','WO3mVNK6+Qe8it5xKbbGzMqHDmII0eqA20xzwXGbG1u0OK53dB9x7yImOVndG9LX','Lisa Anderson',346071668091,'+1234567891','SITE_MANAGER',NULL,'M',1734606068091,1734606068091);
INSERT INTO users VALUES('33ea8644-b644-4da5-b658-cb80c07eba47','manager3@test.com','2UksTrqFHYD8q4sWeuLVNL1jJ1mhRU4We2D3keZPgquit0XV3tFWpMPHsgYSeDJR','James Martinez',693140468092,'+1234567892','SITE_MANAGER',NULL,'M',1734606068092,1734606068092);
INSERT INTO users VALUES('7069ad7e-2e37-4c07-baad-c9ae39520c78','manager4@test.com','RGdNrMQelSVEK1ustWRkFKNzma7ET5ftxWiMqyy5dD9DkSATB5vXun7bXUmCe5Cw','Patricia Lee',756298868093,'+1234567893','SITE_MANAGER',NULL,'F',1734606068093,1734606068093);
INSERT INTO users VALUES('3930ba6a-6722-45fc-aa83-5fce604316e2','manager5@test.com','HVp7vtsn8G/gRqL77fAKX8EC0iSDaaXj03z9sp2qunpbPi9/m/Tym5aNZ/3mrhXf','William Clark',346071668094,'+1234567894','SITE_MANAGER',NULL,'F',1734606068094,1734606068094);
CREATE TABLE sessions(token TEXT PRIMARY KEY,user_id TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY (user_id) REFERENCES users(id));
INSERT INTO sessions VALUES('c9db04b2-cb55-4136-b9c3-80adab9f174e','e4636529-52a7-4647-a080-d21850b88032',1734606088640);
CREATE TABLE locations(id TEXT PRIMARY KEY,address TEXT NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,description TEXT,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,CHECK (latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180));
INSERT INTO locations VALUES('f70e9964-35c6-4eba-8ef8-d997e627b448','2500 Grant Road, Mountain View, CA 94040',37.3975611789025634,-122.076096115467294,'Main entrance, look for blood drive signs',1734606068096,1734606068096);
INSERT INTO locations VALUES('d7571f9d-c04f-4957-a5e6-9b7316b056fc','615 Cuesta Dr, Mountain View, CA 94040',37.404917012072552,-122.071314804965041,'Main entrance, look for blood drive signs',1734606068113,1734606068113);
INSERT INTO locations VALUES('2330dc55-94eb-4759-b42b-1d61b352ab08','1600 Amphitheatre Parkway, Mountain View, CA 94043',37.3859234964277717,-122.089851266986613,'Main entrance, look for blood drive signs',1734606068131,1734606068131);
INSERT INTO locations VALUES('c488b3ec-8c9d-44e7-b844-a54c907fd112','201 Almond Ave, Los Altos, CA 94022',37.3888170631895349,-122.06517903225189,'Main entrance, look for blood drive signs',1734606068183,1734606068183);
INSERT INTO locations VALUES('bd0f353b-c303-4843-9198-36c210b0e3dd','97 Hillview Ave, Los Altos, CA 94022',37.3760081142254279,-122.098272368902442,'Main entrance, look for blood drive signs',1734606068381,1734606068381);
CREATE TABLE events(id TEXT PRIMARY KEY,title TEXT NOT NULL,description TEXT,start_time INTEGER NOT NULL,end_time INTEGER NOT NULL,blood_type_targets TEXT NOT NULL,blood_collected TEXT NOT NULL,host_id TEXT NOT NULL,status TEXT NOT NULL,location_id TEXT NOT NULL,donation_start_time TEXT,donation_end_time TEXT,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,FOREIGN KEY (location_id) REFERENCES locations(id),FOREIGN KEY (host_id) REFERENCES users(id));
INSERT INTO events VALUES('94ce3f7c-5828-4aa8-b8da-e75398389660','Emergency Blood Drive - Mountain View Hospital','Critical blood supplies needed for emergency surgeries. All blood types welcome, especially O-negative. Free health screening for all donors.',1734692468097,1734721268097,'{"O-":33.333333333333336,"A+":33.333333333333336,"B+":33.333333333333336}','{"O-":11.116431309486353,"A+":18.648806310796026,"B+":22.655299316097594}','e4636529-52a7-4647-a080-d21850b88032','UPCOMING','f70e9964-35c6-4eba-8ef8-d997e627b448','09:00','17:00',1734606068098,1734606068098);
INSERT INTO events VALUES('9643b91c-f067-4526-8ac7-343c51b7834a','Community Blood Drive - Cuesta Park','Join our weekend community blood drive. Each donation can save up to three lives! Refreshments provided for all donors.',1734865268114,1734894068114,'{"A+":18.75,"A-":18.75,"B+":18.75,"B-":18.75,"AB+":18.75,"AB-":18.75,"O+":18.75,"O-":18.75}','{"A+":7.7779103781329955,"A-":6.06913402092429,"B+":7.238710392834176,"B-":6.189226350405423,"AB+":10.341124139822037,"AB-":12.355037703613524,"O+":12.857958897764588,"O-":7.739661374147851}','6ba19497-b3b4-4cc5-be17-222457454d09','UPCOMING','d7571f9d-c04f-4957-a5e6-9b7316b056fc','09:00','17:00',1734606068114,1734606068115);
INSERT INTO events VALUES('89d0273d-f254-4ece-9c7c-e2baedc19f02','Tech Companies United Blood Drive','Silicon Valley''s biggest blood drive event. Special focus on rare blood types. Free tech swag for all donors!',1735038068131,1735066868131,'{"AB-":66.66666666666667,"B-":66.66666666666667,"O+":66.66666666666667}','{"AB-":23.43746383805566,"B-":38.28969322664462,"O+":25.638601372204523}','33ea8644-b644-4da5-b658-cb80c07eba47','UPCOMING','2330dc55-94eb-4759-b42b-1d61b352ab08','09:00','17:00',1734606068132,1734606068132);
INSERT INTO events VALUES('16465c22-c259-46f8-a865-f801d178c06d','Student Blood Drive - Los Altos High','Support your community by donating blood. First-time donors welcome! Student ID required for special recognition.',1735210868184,1735239668184,'{"A+":26.666666666666668,"O+":26.666666666666668,"B+":26.666666666666668}','{"A+":14.393725793347366,"O+":18.425910985321323,"B+":14.66455411899226}','7069ad7e-2e37-4c07-baad-c9ae39520c78','UPCOMING','c488b3ec-8c9d-44e7-b844-a54c907fd112','09:00','17:00',1734606068184,1734606068184);
INSERT INTO events VALUES('ebec7ab8-bc4a-47ae-977f-ac297a10b192','Senior Center Blood Drive','Help us meet our community''s blood supply needs. Special assistance available for elderly donors. Morning appointments preferred.',1735383668385,1735412468385,'{"A+":40,"O-":40,"AB+":40}','{"A+":26.295254153414884,"O-":14.46162891090177,"AB+":12.329834475550141}','3930ba6a-6722-45fc-aa83-5fce604316e2','UPCOMING','bd0f353b-c303-4843-9198-36c210b0e3dd','09:00','17:00',1734606068385,1734606068385);
CREATE TABLE registrations(registration_id TEXT PRIMARY KEY,user_id TEXT NOT NULL,event_id TEXT NOT NULL,type TEXT NOT NULL,registration_time INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'ACTIVE',FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE);
INSERT INTO registrations VALUES('b3313e6a-dc91-4e71-ab5f-c10acc2f2a1e','14b40d70-dbf9-4278-b14f-d7bd9bb410cc','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068100,'ACTIVE');
INSERT INTO registrations VALUES('255c9fc6-c9b6-447f-aa78-5433eea4206f','c4eaf133-7ee8-4de4-b6f5-2a80c96d47f0','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068105,'ACTIVE');
INSERT INTO registrations VALUES('222a2e2f-85f8-453c-b956-a7b086288075','25b23930-df24-43ce-802a-27ab5631042d','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068105,'ACTIVE');
INSERT INTO registrations VALUES('9010ebf6-aa58-465c-aed2-0f518fdeb215','dc6e6f49-57b1-46f2-85c6-fd065623e0f1','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068106,'ACTIVE');
INSERT INTO registrations VALUES('cab01cac-4bc8-4bb0-aa4a-bf680f7916c7','faea3b4f-0b2d-416e-9712-88c496295c25','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068106,'ACTIVE');
INSERT INTO registrations VALUES('f347c658-bc24-495e-9527-d3be299403f7','d3e9ab3f-5faa-4a28-bb89-655082a8055d','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068107,'ACTIVE');
INSERT INTO registrations VALUES('ad735492-7778-444f-81d7-c1f21b31afc8','dc4e7d0d-62d9-4395-9f29-cad60997be4f','94ce3f7c-5828-4aa8-b8da-e75398389660','DONOR',1734606068107,'ACTIVE');
INSERT INTO registrations VALUES('4ff55e6e-20c7-4eea-a756-f92f2ae5eec5','1efba027-df1d-4ccc-84a2-ce7b19abb548','94ce3f7c-5828-4aa8-b8da-e75398389660','VOLUNTEER',1734606068108,'ACTIVE');
INSERT INTO registrations VALUES('51d84f8a-3475-4284-8017-880524fc1b32','fcc95821-ad9b-4951-a963-00920127af39','94ce3f7c-5828-4aa8-b8da-e75398389660','VOLUNTEER',1734606068108,'ACTIVE');
INSERT INTO registrations VALUES('2805e425-f070-4814-946c-383a11a29110','04fd8119-43a4-4f5d-ad8a-7c64fb6582c5','94ce3f7c-5828-4aa8-b8da-e75398389660','VOLUNTEER',1734606068108,'ACTIVE');
INSERT INTO registrations VALUES('6ce3dab0-c50f-43da-a69f-87ad2e12d6f5','8e8328f6-b4c2-447d-aec4-259952845b59','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068117,'ACTIVE');
INSERT INTO registrations VALUES('d8b972fd-6aa9-4d87-be77-018e1512254f','162f7504-5297-48e9-92f1-0dce1224549f','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068118,'ACTIVE');
INSERT INTO registrations VALUES('a3b33ea0-da3f-4ffd-be2a-e3d600f0ee01','e5bf577a-ee14-4a8a-9383-78b39f2714f8','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068120,'ACTIVE');
INSERT INTO registrations VALUES('8590e7c5-76b7-47db-9166-625a42a9f349','fc5cc24a-ede2-41a8-8117-4b88fadfeacd','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068121,'ACTIVE');
INSERT INTO registrations VALUES('0cc9b2d6-d15e-426d-b510-278775ccdf15','5b0a3ce4-5a0c-4461-9901-96d4449430fd','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068122,'ACTIVE');
INSERT INTO registrations VALUES('ffe6de70-caae-4b26-b1bc-f8cd7d252337','b3493f81-a2d2-457c-b6ed-8bf6ebdccf65','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068125,'ACTIVE');
INSERT INTO registrations VALUES('24c1959e-bb3d-4b91-aa38-d69eed1d99af','fd2fbbad-0f71-478e-b4aa-e1fceac0fb3c','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068125,'ACTIVE');
INSERT INTO registrations VALUES('28d6728e-f86c-45b7-9923-8e35a3fa5684','19c89adf-2d56-4317-ba88-feb6bce74500','9643b91c-f067-4526-8ac7-343c51b7834a','DONOR',1734606068127,'ACTIVE');
INSERT INTO registrations VALUES('642d97da-0a51-4901-8d55-3a2047caae7d','72ee7d64-9647-407f-a164-db2c2a080dc5','9643b91c-f067-4526-8ac7-343c51b7834a','VOLUNTEER',1734606068128,'ACTIVE');
INSERT INTO registrations VALUES('f2366078-6503-4a9d-9ca5-78ec98b29a5c','b4de9f39-4476-42c0-8105-0b4b0c42bf6a','9643b91c-f067-4526-8ac7-343c51b7834a','VOLUNTEER',1734606068129,'ACTIVE');
INSERT INTO registrations VALUES('d10cca03-edf8-4150-ae64-45007e636659','70211ef8-f12f-45c1-85da-ed255b5d53fc','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068145,'ACTIVE');
INSERT INTO registrations VALUES('e53a0ee0-a3e4-4316-9772-8ba125f0d951','4d05c0bf-c503-4eaf-92fb-7d45e1cdafc8','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068157,'ACTIVE');
INSERT INTO registrations VALUES('554c97f6-6520-4c18-9a74-9c0dd62817e6','d343c8fe-c7af-453a-95e2-6262b7dce3c0','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068158,'ACTIVE');
INSERT INTO registrations VALUES('e34b06bb-690e-4eb2-8022-37e7ef0888c2','a0e3d2c5-d0b0-4fc0-91b4-39d2af1d1615','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068165,'ACTIVE');
INSERT INTO registrations VALUES('db9c86e8-1a3a-410c-8ce5-5c05c4693e95','4a2ee588-fe5e-4d7d-9997-d1347f9f74c5','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068169,'ACTIVE');
INSERT INTO registrations VALUES('8896085d-06d9-4730-8b63-7839238e3d0c','6902822d-df1b-4c60-995c-092be98604e4','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068169,'ACTIVE');
INSERT INTO registrations VALUES('7f281b05-e0c2-4853-94f9-ebc5eb1268d7','5e588461-449b-413c-9f5b-9bc467392908','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068170,'ACTIVE');
INSERT INTO registrations VALUES('474ade3c-92bc-45e3-962e-4a3801c68ab9','ba66adc9-d3d3-45b5-9102-f54b541c113c','89d0273d-f254-4ece-9c7c-e2baedc19f02','DONOR',1734606068170,'ACTIVE');
INSERT INTO registrations VALUES('14133532-6c83-4b17-8c65-da0d5e875484','1c62e60a-ec4e-4be1-88d8-1c59a082e9e6','89d0273d-f254-4ece-9c7c-e2baedc19f02','VOLUNTEER',1734606068171,'ACTIVE');
INSERT INTO registrations VALUES('06148239-4f95-4a1c-9c50-2ab629fc5e5c','c5e3504b-2e08-478e-9149-7cd0515948c3','89d0273d-f254-4ece-9c7c-e2baedc19f02','VOLUNTEER',1734606068171,'ACTIVE');
INSERT INTO registrations VALUES('5afd978f-302a-4ea9-a675-225869e6fa4f','aeb6034d-5db7-4d08-89ce-f777ae385a84','89d0273d-f254-4ece-9c7c-e2baedc19f02','VOLUNTEER',1734606068182,'ACTIVE');
INSERT INTO registrations VALUES('d6cac2b4-55d9-466a-9736-89363e2306dc','982c7167-6dce-43ff-bff8-0e0d5e38541f','89d0273d-f254-4ece-9c7c-e2baedc19f02','VOLUNTEER',1734606068182,'ACTIVE');
INSERT INTO registrations VALUES('d04bf6f4-eaaa-4416-a73d-e6476f23cbcd','0e55fbe5-b527-48da-b26f-e35429774479','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068230,'ACTIVE');
INSERT INTO registrations VALUES('3deaea61-2514-4f3a-aede-7d98e0867a1b','7f1abac3-abb0-45e9-9658-94a66a51918e','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068230,'ACTIVE');
INSERT INTO registrations VALUES('e59987b4-0f2e-44f7-8dac-0366601eacde','e63ecd32-75d4-4461-bf75-a0656a3e0cca','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068232,'ACTIVE');
INSERT INTO registrations VALUES('84ea3413-f7d7-4f0f-8530-4fe121ba2097','aaf0909d-31cc-4880-aa07-4388830557b8','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068238,'ACTIVE');
INSERT INTO registrations VALUES('6f42c088-4441-4c90-88f5-2d8ca6961f33','83a6da5f-d777-4deb-a240-862d917b0276','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068257,'ACTIVE');
INSERT INTO registrations VALUES('d5872403-738f-42a3-8814-7b59e6912046','3e9aa6ad-e260-40a4-bf5c-0d3ae5ce76c0','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068266,'ACTIVE');
INSERT INTO registrations VALUES('6b3719ad-27b1-4aca-9365-58ea834cfaf0','e2d69141-a555-4a2b-896a-7d2d4b3dcf87','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068266,'ACTIVE');
INSERT INTO registrations VALUES('ac6465b3-2c34-40de-8e39-a0e8af2ce212','3d7490a1-f29c-426e-95f0-6bfc0965a2da','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068266,'ACTIVE');
INSERT INTO registrations VALUES('27c8149f-32a8-4667-b9ac-843b60a72b64','752c2a42-68be-4f35-b5c3-edb921c44817','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068267,'ACTIVE');
INSERT INTO registrations VALUES('36e49725-0487-46d1-9b11-c871992fcafb','5e30791a-8a3c-42d3-b3f9-d3a834af6e8b','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068313,'ACTIVE');
INSERT INTO registrations VALUES('fb6304b0-0a06-4ebd-89d3-2debfc764d35','b00940a6-a7d1-452f-9bde-0b4b4002efb6','16465c22-c259-46f8-a865-f801d178c06d','DONOR',1734606068314,'ACTIVE');
INSERT INTO registrations VALUES('5ae9fb27-3837-423d-9029-bcd1aa23cdfd','2a9b2605-98ee-4e44-a72d-4ed72e546f17','16465c22-c259-46f8-a865-f801d178c06d','VOLUNTEER',1734606068349,'ACTIVE');
INSERT INTO registrations VALUES('ea3305a2-3f9d-471c-9b6b-a378c73c2131','617de705-6c66-45a9-ba21-3bbf0af2ed9f','16465c22-c259-46f8-a865-f801d178c06d','VOLUNTEER',1734606068350,'ACTIVE');
INSERT INTO registrations VALUES('32f629e3-364a-4172-ab2f-de4134e6c3f9','2866947b-59fe-4516-adc1-ddbeabcefd8c','16465c22-c259-46f8-a865-f801d178c06d','VOLUNTEER',1734606068361,'ACTIVE');
INSERT INTO registrations VALUES('ffe1c144-fe08-4f5b-bcec-6cc5002dd5c9','259204b7-3f6c-4ade-9b33-b96d9116df34','16465c22-c259-46f8-a865-f801d178c06d','VOLUNTEER',1734606068369,'ACTIVE');
INSERT INTO registrations VALUES('ab492ab7-5933-49d1-90ac-e23fa4f3f1cc','1f57da49-a3bd-465c-abb4-6be160f0cba0','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068386,'ACTIVE');
INSERT INTO registrations VALUES('3a5a49eb-4bfc-45fd-a99d-a181c08c2c72','e31eac58-92b1-4e54-ac0b-3328f17714f4','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068386,'ACTIVE');
INSERT INTO registrations VALUES('3e4b9ae0-ee7a-40d7-98c5-8c50aff7b610','215d2bf7-500a-4bc7-b396-1d1ba44779b9','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068386,'ACTIVE');
INSERT INTO registrations VALUES('2181857b-b44e-4af9-846e-09230cccf983','3a68652a-e3d1-42c1-8ba8-892bbf6dafb3','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068387,'ACTIVE');
INSERT INTO registrations VALUES('8d1cd179-1bff-4096-821b-8843a7a9e94d','35014b26-b871-497b-b16b-0a3c78ba5e1d','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068387,'ACTIVE');
INSERT INTO registrations VALUES('5962bc75-c2fc-4cb7-aaa4-3b921263379d','b29b1448-db26-450b-a39c-81d3922e9d01','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068388,'ACTIVE');
INSERT INTO registrations VALUES('13278e26-f89c-4681-bfb2-7f490641c089','9054df3a-63b5-4bd4-ba90-71c7b4c4b654','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068432,'ACTIVE');
INSERT INTO registrations VALUES('b252343c-565c-427c-9392-b2aed7b09b1b','2962a2dd-78c9-4e6f-81b6-c75b28f14dbc','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068440,'ACTIVE');
INSERT INTO registrations VALUES('0871a4ef-94af-462a-99e4-96ffb0835e1e','f67492a9-41c2-47d9-a073-4e3ebfe6634d','ebec7ab8-bc4a-47ae-977f-ac297a10b192','DONOR',1734606068441,'ACTIVE');
INSERT INTO registrations VALUES('a562dcdc-60d7-4a09-b6f7-151d2e842c71','2a1be136-e24e-4cae-a114-f2f1698c65ba','ebec7ab8-bc4a-47ae-977f-ac297a10b192','VOLUNTEER',1734606068441,'ACTIVE');
INSERT INTO registrations VALUES('e1637f02-fe1f-4ede-b780-fc46572b592f','5d75a112-5ff6-44a0-997b-9e2c3625c6d1','ebec7ab8-bc4a-47ae-977f-ac297a10b192','VOLUNTEER',1734606068441,'ACTIVE');
INSERT INTO registrations VALUES('efcb07d9-ebf3-4279-9216-58ca043df69d','5deefca7-0de6-4fd0-9ea6-fafedf53cce6','ebec7ab8-bc4a-47ae-977f-ac297a10b192','VOLUNTEER',1734606068442,'ACTIVE');
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_location ON events(location_id);
CREATE INDEX idx_events_host ON events(host_id);
CREATE INDEX idx_registrations_user_event ON registrations(user_id, event_id);
CREATE INDEX idx_registrations_status ON registrations(status);
CREATE INDEX idx_registrations_type_status ON registrations(type, status);
CREATE INDEX idx_locations_coords ON locations(latitude, longitude);
CREATE INDEX idx_location_coordinates ON locations(latitude, longitude);
CREATE TRIGGER validate_coordinates BEFORE INSERT ON locations BEGIN     SELECT CASE         WHEN NEW.latitude < -90 OR NEW.latitude > 90 OR              NEW.longitude < -180 OR NEW.longitude > 180         THEN RAISE(ABORT, 'Invalid coordinates')     END; END;
COMMIT;