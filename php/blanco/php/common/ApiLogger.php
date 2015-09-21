<?php

/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/09/14
 * Time: 12:45
 */
class ApiLogger
{
    private static $default_logger;

    private static $initialized = false;

    /**
     * 初期化．最初に呼び出してください．
     *
     * @param array $options
     */
    public static function __init($options = array()) {
        $propfile = $options[0];
        Logger::configure(LIBRARY_PATH . '/etc/' . $propfile);

        // === MDC (_mapped diagnostic contexts_) 設定
        // ※ log4php.appender.{appender_name}.layout.conversionPattern で、'%X{ADDR}' のようにして参照可能
        // http://logging.apache.org/log4php/apidocs/class-LoggerMDC.html
        LoggerMDC::put('ADDR', isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : '-');
        LoggerMDC::put('HOST', isset($_SERVER['REMOTE_HOST']) ? $_SERVER['REMOTE_HOST'] : '-');

        // === ロガー取得
        self::$default_logger = Logger::getLogger('DefaultLogger');

        self::$initialized = true;
    }

    /**
     * trace レベルログの出力します
     *
     * @param $argLog
     */
    public static function trace($argLog) {
        if (self::$initialized) {
            self::$default_logger->trace($argLog);
        }
    }
    /**
     * debug レベルログの出力します
     *
     * @param $argLog
     */
    public static function debug($argLog) {
        if (self::$initialized) {
            self::$default_logger->debug($argLog);
        }
    }
    /**
     * info レベルログの出力します
     *
     * @param $argLog
     */
    public static function info($argLog) {
        if (self::$initialized) {
            self::$default_logger->info($argLog);
        }
    }
    /**
     * warn レベルログの出力します
     *
     * @param $argLog
     */
    public static function warn($argLog) {
        if (self::$initialized) {
            self::$default_logger->warn($argLog);
        }
    }
    /**
     * error レベルログの出力します
     *
     * @param $argLog
     */
    public static function error($argLog) {
        if (self::$initialized) {
            self::$default_logger->error($argLog);
        }
    }
    /**
     * fatal レベルログの出力します
     *
     * @param $argLog
     */
    public static function fatal($argLog) {
        if (self::$initialized) {
            self::$default_logger->fatal($argLog);
        }
    }
}