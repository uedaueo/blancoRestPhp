<?php
/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/09/10
 * Time: 11:18
 */

include_once(__DIR__ . "/env.php");
require_once(LIBRARY_PATH . "/etc/ApiConfig.php");
require_once(LIBRARY_PATH . "/common/ClassLoader.php");

ClassLoader::addPath(
    array(
        LIBRARY_PATH . '/' . ApiConfig::$API_ETC,
        LIBRARY_PATH . '/' . ApiConfig::$API_COMMON,
        LIBRARY_PATH . '/' . ApiConfig::$API_ABST,
        LIBRARY_PATH . '/' . ApiConfig::$API_OBJECTS,
        LIBRARY_PATH . '/' . ApiConfig::$API_IMPLE,
        LIBRARY_PATH . '/' . ApiConfig::$LOG4PHP_DIR
    )
);

spl_autoload_register(array('ClassLoader', '_autoLoad'));

ApiLogger::__init(
    array(
        'log4php.properties'
    ));

try {

    $strApi = $_GET["api"];

    $api = new $strApi();
    $api->action();

} catch (Exception $e) {
    ApiLogger::fatal($e);
}
