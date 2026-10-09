package character;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.smartfoxserver.entities.User;

public class CharacterManager {

    private final Map<Integer, Character> charactersByUserId =
            new HashMap<>();

    // =========================================================
    // REGISTER
    // =========================================================

    public void registerCharacter(Character character) {

        if (character == null) {
            throw new IllegalArgumentException(
                    "Character não pode ser null."
            );
        }

        User user = character.getUser();

        if (user == null) {
            throw new IllegalArgumentException(
                    "Character precisa estar associado a um User."
            );
        }

        int userId = user.getId();

        if (charactersByUserId.containsKey(userId)) {
            throw new IllegalStateException(
                    "Já existe um Character registrado para User ID="
                            + userId
            );
        }

        charactersByUserId.put(
                userId,
                character
        );

        System.out.println(
                "CharacterManager: Character registrado."
                        + " UserID=" + userId
                        + " CharacterID=" + character.getCharacterId()
                        + " Type=" + character.getClass().getSimpleName()
        );
    }

    // =========================================================
    // GET
    // =========================================================

    public Character getByUser(User user) {

        if (user == null) {
            return null;
        }

        return charactersByUserId.get(
                user.getId()
        );
    }

    public Character getByUserId(int userId) {

        return charactersByUserId.get(
                userId
        );
    }

    // =========================================================
    // REMOVE
    // =========================================================

    public Character removeByUser(User user) {

        if (user == null) {
            return null;
        }

        return removeByUserId(
                user.getId()
        );
    }

    public Character removeByUserId(int userId) {

        Character character =
                charactersByUserId.remove(userId);

        if (character != null) {

            System.out.println(
                    "CharacterManager: Character removido."
                            + " UserID=" + userId
                            + " CharacterID=" + character.getCharacterId()
            );
        }

        return character;
    }

    // =========================================================
    // COLLECTION
    // =========================================================

    public Collection<Character> getCharacters() {

        return Collections.unmodifiableCollection(
                charactersByUserId.values()
        );
    }

    // =========================================================
    // INFO
    // =========================================================

    public int getCharacterCount() {

        return charactersByUserId.size();
    }

    public boolean hasCharacter(User user) {

        if (user == null) {
            return false;
        }

        return charactersByUserId.containsKey(
                user.getId()
        );
    }

    public boolean hasCharacter(int userId) {

        return charactersByUserId.containsKey(
                userId
        );
    }
}