<?php
/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/07/05
 * Time: 15:33
 */

abstract class ApiBase
{

    /**
     * 総合エラーコード
     *
     * @var string
     */
    private $apiErrorCode = "";
    /**
     * エラーを格納する配列
     *
     * @var array
     */
    private $apiErrorItems = array();

    private $userSession = null;

    /**
     * 自動生成されたabstractクラスで必ずoverrideされます
     * リクエストIdの名前を返します．
     *
     * @return mixed
     */
    protected abstract function getRequestId();

    /**
     * 自動生成されたabstractクラスで必ずoverrideされます
     * リクエストIdの名前を返します．
     *
     * @return mixed
     */
    protected abstract function getResponseId();

    /**
     * 自動生成されたabstractクラスで必ずoverrideされます
     * APIのメイン関数です
     *
     * @param $argRequest
     * @return mixed
     */
    protected abstract function execute($argRequest);

    /**
     * APIの初期化．
     * 必要に応じてoverrideしてください．
     */
    protected function initialize() {

    }

    /**
     * 自動生成されたabstractクラスでoverrideされる事を想定しています
     */
    protected function validate() {
        return true;
    }

    /**
     * 自動生成されたabstractクラスでoverrideされる事を想定しています
     */
    protected function isAuthenticationRequired() {
        return true;
    }

    private function isAuthenticated() {
        /*
         * COOKIE からセッションIDを取り出して認証する
         */
        if (!isset($_SESSION[Constant::$SESSION_ID])) {
            return false;
        }

        $this->setUserSession($_SESSION[Constant::$SESSION_ID]);

        return true;
    }

    /**
     * API実行
     */
    public function action() {

        ApiLogger::trace("ApiBase#action start : " . get_class($this));

        session_name(Constant::$SESSION_NAME);
        session_start();

        $argRequest = $this->getParamsObject();

        /*
         * Response オブジェクトの作成
         */
        $apiResponse = new RestSampleResponse();

        if ($argRequest !== null) {
            try {
                if ($this->isAuthenticationRequired()) {
                    ApiLogger::debug("Authentication is required.");
                    if (!$this->isAuthenticated()) {
                        throw new AuthenticationException("Authentication Error");
                    }
                }

                if ($this->validate()) {
                    $apiTelegram = $this->execute($argRequest);
                } else {
                    /*
                     * validateion error 時の処理
                     */
                }
            } catch (AuthenticationException $e) {

                ApiLogger::debug("Authentication Exception is caucht.");

            } catch (Exception $e) {

                ApiLogger::debug("General Exception is caught.");
                ApiLogger::fatal($e);

            }
        } else {

            ApiLogger::debug("data cannot get.");

        }

        if (count($this->getApiErrorItems()) !== 0) {

            ApiLogger::debug("ApiBase: Error detected : " . count($this->getApiErrorItems()));

            /*
             * Error detected
             * エラーの取り扱いについては別途実装のこと
             */
        }

        $regResponse = $this->regularizeResponse($apiResponse);

        $jsonString = json_encode($regResponse);

        ApiLogger::debug("Response json : " . $jsonString . "\n");

        if (isset($jsonString)) {
            print $jsonString;
        }
    }

    /**
     * なんでもcast関数．危険なので使わない事．
     *
     * @param $obj
     * @param $toClass
     * @return bool|mixed
     */
//    protected function cast($obj, $toClass) {
//        if (!class_exists($toClass)) {
//            return null;
//        }
//        $length = strlen($toClass);
//        $objIn  = serialize($obj);
//        $objOut = '';
//        if (preg_match('/\AO:\d+:\".*?\":(.*?)\z/', $objIn, $matches)) {
//            $objOut = sprintf('O:%d:"%s":%s', $length, $toClass, $matches[1]);
//        }
//        return unserialize($objOut);
//    }

    /**
     * getter から property 名を推測します
     *
     * @param $getterName
     * @return string
     */
    final protected static function propertyNameFromGetter($getterName) {
        $propName = substr($getterName, 3);
        return mb_strtolower(substr($propName, 0, 1)) . substr($propName, 1);
    }

    /**
     * beanクラス名からpropertyの一覧（配列）を作成します
     *
     * @param $argClassNmae
     * @return array
     */
    final protected static function getProperties($argClassNmae) {
        ApiLogger::trace("getProperties start : " . $argClassNmae);

        $classMethods = get_class_methods($argClassNmae);
        $properties = array();
        foreach ($classMethods as $methodName) {
            if ("get" === substr($methodName, 0, 3)) {
                array_push($properties, self::propertyNameFromGetter($methodName));
            }
        }
        return $properties;
    }

    /**
     * 要求オブジェクトの配列要素を解析します
     *
     * @param $argArray
     */
    final protected static function regularizeArrayRequest($argArray) {
        $regArray = array();

        foreach ($argArray as $element) {
            $type = gettype($element);
            switch ($type) {
                case "object":
                    array_push($regArray, self::regularizeRequest($element, $type));
                    break;
                case "array":
                    array_push($regArray, self::regularizeArrayRequest($element));
                    break;
                default:
                    array_push($regArray, $element);
            }
        }
        return $regArray;
    }

    /**
     * json_decodeが戻した要求オブジェクトをAPIの標準形
     *
     * @param $argObject
     * @param $argClassName
     * @return mixed
     */
    final public static function regularizeRequest($argObject, $argClassName) {
        ApiLogger::trace("regularizeRequest: start");

        if (!$argObject) {
            return null;
        }

        $properties = self::getProperties($argClassName);
        $regObject = new $argClassName();

        foreach ($properties as $property) {
            $setter = "set" . mb_strtoupper(substr($property, 0, 1)) . substr($property, 1);
            $typer = "type" . mb_strtoupper(substr($property, 0, 1)) . substr($property, 1);
            $type = $argClassName::$typer();

            ApiLogger::debug("Setter : " . $setter . ", typer : " . $typer . ", type : " . $type);

            switch ($type) {
                case "object":
                    $regObject->$setter(self::regularizeRequest($argObject->$property, $type));
                    break;
                case "array":
                    $regObject->$setter(self::regularizeArrayRequest($argObject->$property));
                    break;
                default:
                    $regObject->$setter($argObject->$property);
            }
        }

        return $regObject;
    }

    /**
     * JSON文字列で渡されたパラメータをPHP オブジェクトに変換して返します<br>
     * POSTパラメータに data が無い場合や JSON 文字列が不正な場合には null を返します．
     */
    final protected function getParamsObject() {
        ApiLogger::trace("getParamsObject: start");

        if (!array_key_exists('data', $_POST)) {
            ApiLogger::debug("getParamsObject: NO DATA!!!");
            return null;
        }

        $jsonString = $_POST["data"];

        ApiLogger::debug("getParamsObject: Request json = " . $jsonString);

        return self::regularizeRequest(json_decode($jsonString), $this->getRequestId());
    }

    /**
     * 応答オブジェクトの配列要素を解析します
     *
     * @param $argArray
     */
    final protected static function regularizeArrayResponse($argArray) {
        $regArray = array();

        foreach ($argArray as $element) {
            $type = gettype($element);
            switch ($type) {
                case "object":
                    array_push($regArray, self::regularizeResponse($element));
                    break;
                case "array":
                    array_push($regArray, self::regularizeArrayResponse($element));
                    break;
                default:
                    array_push($regArray, $element);
            }
        }
        return $regArray;
    }

    /**
     * 応答オブジェクトを正規化します
     *
     * @param $argObject
     * @return null|stdClass
     */
    final public static function regularizeResponse($argObject) {

        $classMethods = get_class_methods($argObject);

        if (!$classMethods) {
            return null;
        }

        $regObject = new stdClass();

        foreach ($classMethods as $methodName) {
            if ("get" === substr($methodName, 0, 3)) {
                $property = self::propertyNameFromGetter($methodName);
                $value = $argObject->$methodName();
                if ($value !== null) {
                    $type = gettype($value);
                    switch ($type) {
                        case "object":
                            $regObject->$property = self::regularizeResponse($value);
                            break;
                        case "array":
                            $regObject->$property = self::regularizeArrayResponse($value);
                            break;
                        default:
                            $regObject->$property = $value;
                    }
                }
            }
        }

        return $regObject;
    }

    /**
     * Errors に Error を追加します
     *
     * @param $argApiErros
     */
    final protected function setApiErrorItem($argErroItem) {
        array_push($this->apiErrorItems, $argErroItem);
    }

    /**
     * Errors に登録された全Errorを取得します
     *
     * @return array
     */
    final protected function getApiErrorItems() {
        return $this->apiErrorItems;
    }

    /**
     * Errors全体のエラーコードをセットします
     *
     * @param $argApiErrorCode
     */
    final protected function setApiErrorCode($argApiErrorCode) {
        $this->apiErrorCode = $argApiErrorCode;
    }

    /**
     * Errors全体のエラーコードを取得します
     *
     * @return string
     */
    final protected function getApiErrorCode() {
        return $this->apiErrorCode;
    }

    /**
     * @return null
     */
    public function getUserSession()
    {
        return $this->userSession;
    }

    /**
     * @param null $userSession
     */
    public function setUserSession($userSession)
    {
        $this->userSession = $userSession;
    }

}
