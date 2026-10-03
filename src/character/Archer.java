package character;

import com.smartfoxserver.entities.User;

public class Archer extends Character {

    public Archer(
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