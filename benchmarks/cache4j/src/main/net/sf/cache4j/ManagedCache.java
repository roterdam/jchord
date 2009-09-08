/* =========================================================================
 * File: $Id: $ManagedCache.java,v$
 *
 * Copyright (c) 2006, Yuriy Stepovoy. All rights reserved.
 * email: stepovoy@gmail.com
 *
 * =========================================================================
 */

package net.sf.cache4j;

import net.sf.cache4j.CacheException;
import net.sf.cache4j.CacheConfig;

/**
 * ��������� ManagedCache ������������� ������ ���������� ����������� ����
 *
 * @version $Revision: 1.0 $ $Date:$
 * @author Yuriy Stepovoy. <a href="mailto:stepovoy@gmail.com">stepovoy@gmail.com</a>
 **/

public interface ManagedCache {
// ----------------------------------------------------------------------------- ���������
// ----------------------------------------------------------------------------- �������� ������
// ----------------------------------------------------------------------------- ����������� ����������
// ----------------------------------------------------------------------------- ������������
// ----------------------------------------------------------------------------- Public ������

    /**
     * ������������� ������������ ����.
     * ��� ��������� ������������ ��� ������� ���� ��������.
     * @param config ������������
     * @throws CacheException ���� �������� ��������
     */
    public void setCacheConfig(CacheConfig config) throws CacheException;

    /**
     * ��������� ������� ����.
     * ��������� ������� � ������� ����������� ����� ����� ��� ��������� �����
     * �����������.
     * @throws CacheException ���� �������� ��������
     */
    public void clean() throws CacheException;

// ----------------------------------------------------------------------------- Package scope ������
// ----------------------------------------------------------------------------- Protected ������
// ----------------------------------------------------------------------------- Private ������
// ----------------------------------------------------------------------------- Inner ������
}

/*
$Log: ManagedCache.java,v $
*/
