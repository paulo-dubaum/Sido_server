package character;

import com.smartfoxserver.entities.User;

public class Mage extends Character {

    public Mage(
            long characterId,
            User user,
            String name,
            float x,
            float y,
            float z) {

        super(
                characterId,
                user,
                name,
                x,
                y,
                z
        );
    }
}