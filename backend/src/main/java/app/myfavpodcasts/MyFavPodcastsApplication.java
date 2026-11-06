package app.myfavpodcasts;

import app.myfavpodcasts.shared.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
public class MyFavPodcastsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyFavPodcastsApplication.class, args);
    }
}
