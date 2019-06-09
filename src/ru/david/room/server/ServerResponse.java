package ru.david.room.server;

/**
 * Инкапсулирует ответ от сервера при различных запросах
 */
class ServerResponse {
    enum Registration {
        OK, ADDRESS_ALREADY_REGISTERED,
        INCORRECT_EMAIL, INCORRECT_NAME,
        INCORRECT_PASSWORD, DB_NOT_SUPPORTED,
        INTERNAL_ERROR, ADDRESS_IN_USE
    }

    static class Login {
        enum ResponseType {
            OK, WRONG_PASSWORD, DB_NOT_SUPPORTED, INTERNAL_ERROR
        }
        ResponseType responseType;
        int token;
        int userid;
        String name;
    }

    enum AcceptRegistrationToken {
        OK, WRONG_TOKEN, DB_NOT_SUPPORTED, INTERNAL_ERROR
    }

    enum ChangePassword {
        OK, WRONG_TOKEN, WRONG_PASSWORD, INCORRECT_NEW_PASSWORD, DB_NOT_SUPPORTED, INTERNAL_ERROR
    }

    enum RequestPasswordReset {
        OK, EMAIL_NOT_EXIST, DB_NOT_SUPPORTED, INTERNAL_ERROR
    }

    enum ResetPassword {
        OK, WRONG_TOKEN, INCORRECT_PASSWORD, DB_NOT_SUPPORTED, INTERNAL_ERROR
    }

    enum AddCreature {
        OK, NOT_ENOUGH_SPACE, NOT_AUTHORIZED, DB_NOT_SUPPORTED, INTERNAL_ERROR
    }

    static class RemoveCreature {
        enum ResponseType {
            OK, NOT_AUTHORIZED, DB_NOT_SUPPORTED, INTERNAL_ERROR
        }
        int updates;
        ResponseType type;
    }
}
