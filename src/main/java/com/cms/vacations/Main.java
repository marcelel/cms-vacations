package com.cms.vacations;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import com.cms.vacations.activities.ActivitiesKafkaService;
import com.cms.vacations.activities.KafkaProducerSettingsFactory;
import com.cms.vacations.events.EventDummyService;
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
        system = ActorSystem.create("vacations");
    }

    private static void initializeHttpServer() {
        MongoConfiguration mongoConfiguration = new MongoConfiguration();
        MongoDatabase mongoDatabase = mongoConfiguration.create();
        MongoDataStore<Vacation> vacationsDataStore =
                new MongoDataStore<>(system, "vacations", mongoDatabase, Vacation.class);
        VacationMongoRepository vacationMongoRepository = new VacationMongoRepository(vacationsDataStore);
        MongoDataStore<User> usersDataStore =
                new MongoDataStore<>(system, "users", mongoDatabase, User.class);
        UserMongoRepository userMongoRepository = new UserMongoRepository(usersDataStore);
        EventDummyService eventDummyService = new EventDummyService();
        ActivitiesKafkaService activitiesKafkaService = new ActivitiesKafkaService(system, new KafkaProducerSettingsFactory(system).create(), "activities");
        ActorRef userActorSupervisor = system.actorOf(UserActorSupervisor.create(userMongoRepository,
                vacationMongoRepository, eventDummyService, activitiesKafkaService, new VacationDaysCalculator(List.of())));
        VacationRoutes routes = new VacationRoutes(userActorSupervisor);

        int httpPort = system.settings()
            .config()
            .getInt("akka.http.server.default-http-port");

        Http.get(system)
            .newServerAt("localhost", httpPort)
            .bind(routes.createRoutes());
    }
}
