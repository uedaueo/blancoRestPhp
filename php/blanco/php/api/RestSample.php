<?php

/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/09/13
 * Time: 17:41
 */
class RestSample extends AbstractRestSample
{

    protected function process($argRestSampleRequest) {

        ApiLogger::trace("RestSample#process start : ");

        /*
         * 複数台のサーバがある場合は，セッション情報は DB に格納
         * かつセッションidはワンタイムにする
         */
        if (isset($_SESSION[Constant::$SESSION_ID])) {
            unset($_SESSION[Constant::$SESSION_ID]);
        }

        $cookieSession = new CookieSession();
        $cookieSession->setUserId("hoge");
        $cookieSession->setLang("ja");
        $cookieSession->setSessionToken("");
        $_SESSION[Constant::$SESSION_ID] = $cookieSession;

        ApiLogger::debug("Session installed.");

        $restSampleResponse = new RestSampleResponse();

        ApiLogger::debug("RestSample#process end");

        return $restSampleResponse;
    }
}