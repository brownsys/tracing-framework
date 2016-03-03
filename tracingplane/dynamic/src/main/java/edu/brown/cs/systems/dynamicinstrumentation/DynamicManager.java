package edu.brown.cs.systems.dynamicinstrumentation;

import java.lang.instrument.UnmodifiableClassException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;

/** Keeps track of modifications made */
public class DynamicManager {
    
    public final Agent agent;
    public final Collection<Throwable> problems = Lists.newArrayList();
    public final ChangeSet pending = new ChangeSet();
    public final Multimap<String, DynamicModification> installed = HashMultimap.create();
    
    public DynamicManager(Agent agent) {
        this.agent = agent;
    }
    
    public DynamicManager add(DynamicModification modification) {
        pending.add(modification);
        return this;
    }
    
    public DynamicManager addAll(Collection<DynamicModification> modifications) {
        for (DynamicModification m : modifications) {
            add(m);
        }
        return this;
    }
    
    public DynamicManager remove(DynamicModification modification) {
        pending.remove(modification);
        return this;
    }
    
    public DynamicManager removeAll(Collection<DynamicModification> modifications) {
        for (DynamicModification m : modifications) {
            remove(m);
        }
        return this;
    }
    
    public DynamicManager refresh(String className) {
        pending.toRefresh.add(className);
        return this;
    }
    
    public DynamicManager reset(String className) {
        pending.toRemove.addAll(installed.get(className));
        return this;
    }
    
    public DynamicManager cancelPending() {
        pending.cancel();
        return this;
    }
    
    public DynamicManager clear() {
        pending.cancel();
        pending.toRemove.addAll(installed.values());
        installed.clear();
        return this;
    }
    
    public void install() throws CannotCompileException, UnmodifiableClassException {
        if (agent != null) {
            problems.clear();
            agent.install(pending.changes(), problems);
            pending.persist();
        }
    }
    
    public class ChangeSet {
        
        public final Set<String> toRefresh = Sets.newHashSet();
        public final Set<DynamicModification> toAdd = Sets.newHashSet();
        public final Set<DynamicModification> toRemove = Sets.newHashSet();
        
        public void add(DynamicModification modification) {
            if (toRemove.contains(modification)) {
                toRemove.remove(modification);
            } else {
                toAdd.add(modification);
            }
        }
        
        public void remove(DynamicModification modification) {
            if (toAdd.contains(modification)) {
                toAdd.remove(modification);
            } else {
                toRemove.add(modification);
            }
        }
        
        public Set<String> affects() {
            Set<String> affected = Sets.newHashSet(toRefresh);
            for (DynamicModification m : toAdd) {
                affected.addAll(m.affects());
            }
            for (DynamicModification m : toRemove) {
                affected.addAll(m.affects());
            }
            return affected;
        }
        
        public Map<String, Collection<DynamicModification>> changes() {
            // Get the modifications for the affected classes
            Map<String, Collection<DynamicModification>> toInstall = Maps.newHashMap();
            for (String className : affects()) {
                toInstall.put(className, Sets.newHashSet(installed.get(className)));
            }
            
            // Remove the toRemove
            for (DynamicModification m : toRemove) {
                for (String affected : m.affects()) {
                    toInstall.get(affected).remove(m);
                }
            }
            
            // Add the toAdd
            for (DynamicModification m : toAdd) {
                for (String affected : m.affects()) {
                    toInstall.get(affected).add(m);
                }
            }
            
            return toInstall;
        }
        
        public void persist() {
            for (DynamicModification m : toRemove) {
                for (String affected : m.affects()) {
                    installed.remove(affected, m);
                }
            }
            for (DynamicModification m : toAdd) {
                for (String affected : m.affects()) {
                    installed.put(affected, m);
                }
            }
            toRemove.clear();
            toAdd.clear();
            toRefresh.clear();
        }
        
        public void cancel() {
            toRemove.clear();
            toAdd.clear();
            toRefresh.clear();
        }
        
    }
    
}
