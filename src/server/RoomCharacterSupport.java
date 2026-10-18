package server;

import com.smartfoxserver.entities.User;
import character.Character;

/** Operacoes de personagem isoladas por instancia de MMORoom. */
final class RoomCharacterSupport {
    private RoomCharacterSupport() {}

    static void remove(User user, RoomSimulationContext context) {
        synchronized (context) {
            Character character = context.getCharacterManager().getByUser(user);
            if (character == null) return;
            // Primeiro avisa a IA: evita alvos pendurados durante a remocao.
            context.getNpcManager().onCharacterDisconnected(character);
            context.getCharacterManager().removeByUser(user);
        }
    }
}
