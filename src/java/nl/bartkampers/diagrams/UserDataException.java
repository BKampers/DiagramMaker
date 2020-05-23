/*
** © Bart Kampers
*/

package nl.bartkampers.diagrams;


class UserDataException extends Exception{


    UserDataException(String message) {
        super(message);
    }


    UserDataException(Exception cause) {
        super(cause);
    }


    UserDataException(String message, Exception cause) {
        super(message, cause);
    }


}
