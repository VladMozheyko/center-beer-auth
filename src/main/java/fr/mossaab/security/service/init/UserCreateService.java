package fr.mossaab.security.service.init;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class UserCreateService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void createUsers() {
        if (userRepository.count() == 0) {
            createUser(500L, "Sasha", "Kichmarev", "kichmarev@list.ru",
                    "$2a$10$JsvXJV3NoRMHn0uG8k7IRuXHb4BAL5I17h4wS/BGgC9v6RqxpGSBq",
                    "+78005553555", 0, "2000-01-01", "SELLER",null);

            createUser(501L, "Владислав", "Можейко", "Vlad72229@yandex.ru",
                    "$2a$10$QGl4Wtd20zVUu3BRYqBs5uGCsWDE0rvabE2I/XBWxQl0/NOdGwILS",
                    "89683725778", 1600, "1998-08-07", "INSTALLER",null);

            createUser(502L, "Оксана", "Сардоникова", "terinaanko@gmail.com",
                    "$2a$10$n0B88AoO3z4PJOncKJSrguASu9aalBGBKqOoasVEIrfEiyXcxsHWa",
                    "+75204615401", 0, "1993-09-06", "SELLER",null);

            createUser(503L, "Евгений", "Се", "sparco.russia@gmail.com",
                    "$2a$10$LYi6CeqhXelrrCWkP2z8oOVNJU25DhtJwdAtNqBg1GUoCpAR/l8bq",
                    "89287677704", 0, "1995-06-21", "RETAIL_CUSTOMER",null);

            createUser(504L, "Сергей", "Поляков", "i@poliacov.ru",
                    "$2a$10$4JEZtj7Ktv0fDJ1qUE610epdTbcNO21i7A325.DpQfXmxspoNTyy6",
                    "+79026938337", 0, "1973-07-02", "RETAIL_CUSTOMER",null);

            createUser(505L, "Оля", "Литвинова", "alukard4596@gmail.com",
                    "$2a$10$OwqcNihwd5j3r2mkmlYOY.LiS29AxaAKVIQ6uQQc7KOEXlmC3xaf6",
                    "89785678757", 0, "1996-05-15", "SELLER",null);
        }
    }

    private void createUser(Long id, String firstname, String lastname, String email,
                            String password, String phoneNumber, int balance,
                            String dateOfBirthStr, String workerRoleStr,String activationCode) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date dateOfBirth = format.parse(dateOfBirthStr);

            User user = User.builder()
                    .id(id) // Предполагается, что id уже задан
                    .firstname(firstname)
                    .lastname(lastname)
                    .email(email)
                    .password(password) // Пароль уже зашифрован
                    .phoneNumber(phoneNumber)
                    .balance(balance)
                    .dateOfBirth(dateOfBirth)
                    .workerRoles(WorkerRole.valueOf(workerRoleStr))
                    .role(Role.USER)
                    .activationCode(activationCode)
                    .build();

            userRepository.save(user);
        } catch (Exception e) {
            // Обработка ошибок (например, вывод в лог)
            e.printStackTrace();
        }
    }
}
