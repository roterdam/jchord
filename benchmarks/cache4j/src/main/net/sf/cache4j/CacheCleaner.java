/* =========================================================================
 * File: CacheCleaner.java$
 *
 * Copyright (c) 2006, Yuriy Stepovoy. All rights reserved.
 * email: stepovoy@gmail.com
 *
 * =========================================================================
 */

package net.sf.cache4j;


/**
 * Класс CacheCleaner выполняет очистку устаревших объектов
 */

public class CacheCleaner extends Thread {
// ----------------------------------------------------------------------------- Константы
// ----------------------------------------------------------------------------- Атрибуты класса

    /**
     * Интервал очистки
     */
    private long _cleanInterval;

    /**
     * true если поток находится в спячке
     */
    private boolean _sleep = false;

// ----------------------------------------------------------------------------- Статические переменные
// ----------------------------------------------------------------------------- Конструкторы

    /**
     * Конструктор
     * @param cleanInterval интервал(в миллисекундах) с которым нужно выполнять очистку
     */
    public CacheCleaner(long cleanInterval) {
        _cleanInterval = cleanInterval;

        setName(this.getClass().getName());
        setDaemon(true);
        //устанавливать минимальный приоритет не нужно потому что удаление устаревших
        //объектов не менее важная задача
        //setPriority(Thread.MIN_PRIORITY);
    }

// ----------------------------------------------------------------------------- Public методы

    /**
     * Устанавливает интервал очистки
     * @param cleanInterval интервал(в миллисекундах) с которым нужно выполнять очистку
     */
    public void setCleanInterval(long cleanInterval) {
        _cleanInterval = cleanInterval;

        synchronized(this){
            if(_sleep){
                interrupt();
            }
        }
    }

    /**
     * Основной метод. Для всех кешей вызывается метод <code>clean</code>
     */
    public void run() {
        while(true)  {
            try {
                CacheFactory cacheFactory = CacheFactory.getInstance();
                Object[] objIdArr = cacheFactory.getCacheIds();
                for (int i = 0, indx = objIdArr==null ? 0 : objIdArr.length; i<indx; i++) {
                    ManagedCache cache = (ManagedCache)cacheFactory.getCache(objIdArr[i]);
                    if(cache!=null){
                        cache.clean();
                    }
                    yield();
                }
            } catch (Throwable t){
                t.printStackTrace();
            }

            _sleep = true;
            try {
                sleep(_cleanInterval);
            } catch (Throwable t){
            } finally {
                _sleep = false;
            }
        }
    }

// ----------------------------------------------------------------------------- Package scope методы
// ----------------------------------------------------------------------------- Protected методы
// ----------------------------------------------------------------------------- Private методы
// ----------------------------------------------------------------------------- Inner классы

}

/*
$Log: CacheCleaner.java,v $
*/
