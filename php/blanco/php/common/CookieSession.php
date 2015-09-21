<?php

/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/09/14
 * Time: 0:33
 *
 * ユーザセッションは以下の形式で COOKIE に文字列として保管される
 * 1) serialize 後，base64 encoded
 * 2) CRC32 checksum をつける
 */
class CookieSession
{
    private $userId;
    private $sessionToken;
    private $lang;

    /**
     * @return mixed
     */
    public function getLang()
    {
        return $this->lang;
    }

    /**
     * @param mixed $lang
     */
    public function setLang($lang)
    {
        $this->lang = $lang;
    }

    /**
     * @return mixed
     */
    public function getUserId()
    {
        return $this->userId;
    }

    /**
     * @param mixed $userId
     */
    public function setUserId($userId)
    {
        $this->userId = $userId;
    }

    /**
     * @return mixed
     */
    public function getSessionToken()
    {
        return $this->sessionToken;
    }

    /**
     * @param mixed $sessionToken
     */
    public function setSessionToken($sessionToken)
    {
        $this->sessionToken = $sessionToken;
    }

}