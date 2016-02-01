package org.novapeng.jpax;

import java.lang.instrument.Instrumentation;

/**
 *
 * Created by pengchangguo on 15/11/16.
 */
@SuppressWarnings("unused")
@Deprecated
public class AgentMain {

    public static void premain(String args, Instrumentation ins) {
        System.out.println("execute Pre Main...............");
        //noinspection deprecation
        ins.addTransformer(new JPAClassFileTransformer());
    }

    public static void agentmain(String args, Instrumentation ins) {
        System.out.println("execute Agent Main...............");
    }
}
