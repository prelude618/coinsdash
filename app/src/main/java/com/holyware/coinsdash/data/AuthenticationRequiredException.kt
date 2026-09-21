package com.holyware.coinsdash.data

class AuthenticationRequiredException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UsernameTakenException : Exception("이미 사용 중인 아이디입니다. 다른 아이디를 입력하세요.")
class UsernameInvalidException : Exception("아이디는 3~20자의 한글·영문·숫자와 ., _, -만 사용할 수 있습니다.")
class UsernameAlreadySetException : Exception("이미 아이디가 설정된 계정입니다.")
