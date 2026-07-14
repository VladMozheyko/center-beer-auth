package fr.mossaab.security.unit.service.social.handler;

import fr.mossaab.security.service.social.hendler.OAuthStateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit-test для OAuthStateStorage")
class OAuthStateStorageTest {

    private OAuthStateStorage stateStorage;

    @BeforeEach
    void setUp() {
        stateStorage = new OAuthStateStorage();
    }

    @Test
    @DisplayName("Сохраняет и получает state")
    void putAndGetString() {
        String springState = "test-state-123";
        String customState = "custom-state-456";

        stateStorage.put(springState, customState);
        String result = stateStorage.get(springState);

        assertNotNull(result);
        assertEquals(customState, result);
    }

    @Test
    @DisplayName("Возвращает null для несуществующего state")
    void getNonExistentState() {
        String nonExistentState = "non-existent-state";

        String result = stateStorage.get(nonExistentState);

        assertNull(result);
    }

    @Test
    @DisplayName("Удаляет state из кэша")
    void removeState() {
        String springState = "test-state-to-remove";

        stateStorage.put(springState, "value");
        stateStorage.remove(springState);

        String result = stateStorage.get(springState);
        assertNull(result);
    }

    @Test
    @DisplayName("contains возвращает true для существующего state")
    void containsExistingState() {
        String springState = "test-state-contains";

        stateStorage.put(springState, "value");
        boolean result = stateStorage.contains(springState);

        assertTrue(result);
    }

    @Test
    @DisplayName("contains возвращает false для несуществующего state")
    void containsNonExistentState() {
        String nonExistentState = "non-existent-state";

        boolean result = stateStorage.contains(nonExistentState);

        assertFalse(result);
    }

    @Test
    @DisplayName("Перезаписывает существующий state")
    void overwriteState() {
        String springState = "test-state-overwrite";

        stateStorage.put(springState, "original-value");
        stateStorage.put(springState, "new-value");

        String result = stateStorage.get(springState);
        assertNotNull(result);
        assertEquals("new-value", result);
    }
}
