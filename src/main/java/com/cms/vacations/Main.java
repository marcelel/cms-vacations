package com.cms.vacations;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.http.javadsl.Http;
import com.cms.vacations.activities.ActivitiesKafkaService;
import com.cms.vacations.activities.KafkaProducerSettingsFactory;
import com.cms.vacations.events.EventClient;
import com.cms.vacations.events.EventRestService;
import com.cms.vacations.messages.VacationShardingMessageExtractor;
import com.cms.vacations.mongo.MongoConfiguration;
import com.cms.vacations.mongo.MongoDataStore;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static ActorSystem system;

    public static void main(String[] args) {
        loadConfigOverrides(args);

        initializeActorSystem();
        initializeHttpServer();
    }

    private static void loadConfigOverrides(String[] args) {
        String regex = "-D(\\S+)=(\\S+)";
        Pattern pattern = Pattern.compile(regex);

        for (String arg : args) {
            Matcher matcher = pattern.matcher(arg);

            while(matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                logger.info("Config Override: "+key+" = "+value);
                System.setProperty(key, value);
            }
        }
    }

    private static void initializeActorSystem() {
        system = ActorSystem.create("Vacations");
    }

    private static void initializeHttpServer() {
        MongoConfiguration mongoConfiguration = new MongoConfiguration();
        MongoDatabase mongoDatabase = mongoConfiguration.create();

        MongoDataStore<Vacation> vacationsDataStore =
                new MongoDataStore<>(system, "vacations", mongoDatabase, Vacation.class);
        VacationMongoRepository vacationRepository = new VacationMongoRepository(vacationsDataStore);

        MongoDataStore<User> usersDataStore =
                new MongoDataStore<>(system, "users", mongoDatabase, User.class);
        UserMongoRepository userRepository = new UserMongoRepository(usersDataStore);

        EventService eventService = new EventRestService(new EventClient(system, Http.get(system), "http://localhost:8000"));

        ActivitiesKafkaService activityService = new ActivitiesKafkaService(system, new KafkaProducerSettingsFactory(system).create(), "activities");

        VacationDaysCalculator vacationDaysCalculator = new VacationDaysCalculator(List.of());

        ActorRef userActorSupervisor = ClusterSharding.get(system).start(
                "vacationShardedActor",
                UserActor.create(
                        userRepository, vacationRepository, eventService, activityService, vacationDaysCalculator
                ),
                ClusterShardingSettings.create(system),
                new VacationShardingMessageExtractor(30)
        );
        VacationRoutes routes = new VacationRoutes(userActorSupervisor);

        int httpPort = system.settings()
                .config()
                .getInt("akka.http.server.default-http-port");

        Http.get(system)
                .newServerAt("localhost", httpPort)
                .bind(routes.createRoutes());
    }
}
