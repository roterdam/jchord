/* =========================================================================
 * File: $Id: $Configurator.java,v$
 *
 * Copyright (c) 2006, Yuriy Stepovoy. All rights reserved.
 * email: stepovoy@gmail.com
 *
 * =========================================================================
 */

package net.sf.cache4j.impl;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import net.sf.cache4j.impl.BlockingCache;
import net.sf.cache4j.impl.CacheConfigImpl;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.InputStream;

import net.sf.cache4j.CacheConfig;
import net.sf.cache4j.CacheException;
import net.sf.cache4j.CacheFactory;
import net.sf.cache4j.Cache;
import net.sf.cache4j.ManagedCache;

/**
 * ����� Configurator ��������� �������� XML ������������
 *
 * @version $Revision: 1.0 $ $Date:$
 * @author Yuriy Stepovoy. <a href="mailto:stepovoy@gmail.com">stepovoy@gmail.com</a>
 **/

public class Configurator {
// ----------------------------------------------------------------------------- ���������
// ----------------------------------------------------------------------------- �������� ������
// ----------------------------------------------------------------------------- ����������� ����������
    private final static long SECOND = 1000;
    private final static long MINUTE = SECOND * 60;
    private final static long HOUR   = MINUTE * 60;

    private final static long KB = 1024;
    private final static long MB = KB * 1024;

// ----------------------------------------------------------------------------- ������������
// ----------------------------------------------------------------------------- Public ������

    /**
     * ��������� ������������. ��� ���� ��������� � ������������ ����������� �
     * {@link net.sf.cache4j.CacheFactory}.
     * @param in �������� ����� � XML �������������
     * @throws CacheException ���� ������� ������ � ������������
     */
    public static void loadConfig(InputStream in) throws CacheException {
        CacheFactory cf = CacheFactory.getInstance();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);

            NodeList nodeList = document.getChildNodes();
            Node node = nodeList==null || nodeList.getLength()==0 ? null : nodeList.item(0);

            //�������� ���� ������ ���������� cache-config
            if (node==null || !"cache-factory".equalsIgnoreCase(node.getNodeName())) {
                throw new CacheException("root node must be \"cache-factory\"");
            }

            if ((node instanceof Element)) {
                long cleanInteval = getTimeLong(((Element)node).getAttribute("clean-interval"));
                if(cleanInteval>0){
                    cf.setCleanInterval(cleanInteval);
                } else {
                    //�� ��������� 30 ������
                    cf.setCleanInterval(30000); //30sec
                }
            }

            for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ((n instanceof Element) && "cache".equalsIgnoreCase(n.getNodeName())) {
                    Cache cache = null;
                    CacheConfig config = null;

                    String id = ((Element)n).getAttribute("id");
                    String desc = ((Element)n).getAttribute("desc");
                    long ttl = getTimeLong(((Element)n).getAttribute("ttl"));
                    long idle = getTimeLong(((Element)n).getAttribute("idle"));
                    long maxMemorySize = getCapacityLong(((Element)n).getAttribute("max-memory-size"));
                    int maxSize = getInt(((Element)n).getAttribute("max-size"));

                    String type = ((Element)n).getAttribute("type");
                    if(type==null || type.trim().length()==0){
                        type = "synchronized";
                    }
                    type = type.trim().toLowerCase();
                    if(type.equals("blocking")){
                        cache = new BlockingCache();
                    } else if(type.equals("synchronized")) {
                        cache = new SynchronizedCache();
                    } else if(type.equals("nocache")) {
                        cache = new EmptyCache();
                    } else {
                        throw new CacheException("Unknown cache type:"+type);
                    }

                    String algorithm = ((Element)n).getAttribute("algorithm");
                    if(algorithm==null || algorithm.trim().length()==0){
                        algorithm = "lru";
                    }
                    algorithm = algorithm.trim().toLowerCase();
                    if(!algorithm.equals(CacheConfigImpl.LRU) &&
                       !algorithm.equals(CacheConfigImpl.LFU) &&
                       !algorithm.equals(CacheConfigImpl.FIFO) ) {
                        throw new CacheException("Unknown cache algorithm:"+algorithm);
                    }

                    String reference = ((Element)n).getAttribute("reference");
                    if(reference==null || reference.trim().length()==0){
                        reference = "strong";
                    }
                    reference = reference.trim().toLowerCase();
                    if(!reference.equals("strong") && !reference.equals("soft") ) {
                        throw new CacheException("Unknown cache object reference:"+reference);
                    }

                    config = new CacheConfigImpl(id, desc, ttl, idle, maxMemorySize, maxSize, type, algorithm, reference);
                    ((ManagedCache)cache).setCacheConfig(config);

                    cf.addCache(cache);
                }
            }

        } catch (SAXParseException e) {
            String msg = "Parsing error, line " + e.getLineNumber() + ", uri " + e.getSystemId()+"\n"+
                         "   " + e.getMessage();
            throw new CacheException(msg);
        } catch (Exception e) {
            throw new CacheException(e.getMessage());
        }
    }

// ----------------------------------------------------------------------------- Package scope ������
// ----------------------------------------------------------------------------- Protected ������
// ----------------------------------------------------------------------------- Private ������

    /**
     * ����������� ������ � �����.
     * @param value ������
     * @return ���������� �����. ���� �������� ������ ������ ��� null
     * ������������ 0.
     */
    private static int getInt(String value){
        if(value==null || value.trim().length()==0) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    /**
     * ����������� ������ � long. � ����� ������ ����� ���������:
     * s-�������, m-������, h-����.
     * @param value ������
     * @return ���������� ����� �����������. ���� �������� ������ ������ ��� null
     * ������������ 0.
     */
    private static long getTimeLong(String value){
        if(value==null || value.trim().length()==0) {
            return 0;
        }
        value = value.trim().toLowerCase();
        String lastSym = value.substring(value.length()-1, value.length());
        if(lastSym.equalsIgnoreCase("s")){
            return Long.parseLong(value.substring(0, value.length()-1))*SECOND;
        } else if(lastSym.equalsIgnoreCase("m")){
            return Long.parseLong(value.substring(0, value.length()-1))*MINUTE;
        } if(lastSym.equalsIgnoreCase("h")){
            return Long.parseLong(value.substring(0, value.length()-1))*HOUR;
        } else {
            return Long.parseLong(value);
        }
    }

    /**
     * ����������� ������ � long. � ����� ������ ����� ���������: k-��������,
     * m-��������, g-��������.
     * @param value ������
     * @return ���������� ����� ����. ���� �������� ������ ������ ��� null
     * ������������ 0.
     */
    private static long getCapacityLong(String value){
        if(value==null || value.trim().length()==0) {
            return 0;
        }
        value = value.trim().toLowerCase();
        String lastSym = value.substring(value.length()-1, value.length());
        if(lastSym.equalsIgnoreCase("k")){
            return Long.parseLong(value.substring(0, value.length()-1))*KB;
        } else if(lastSym.equalsIgnoreCase("m")){
            return Long.parseLong(value.substring(0, value.length()-1))*MB;
        } else {
            return Long.parseLong(value);
        }
    }

// ----------------------------------------------------------------------------- Inner ������
}

/*
$Log: Configurator.java,v $
*/
