package com.pmease.commons.hibernate;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.NamingStrategy;

import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.pmease.commons.hibernate.dao.DefaultDao;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.jackson.HibernateObjectMapperConfigurator;
import com.pmease.commons.jackson.ObjectMapperConfigurator;
import com.pmease.commons.jetty.ServletConfigurator;
import com.pmease.commons.loader.AbstractPlugin;
import com.pmease.commons.loader.AbstractPluginModule;

public class HibernateModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();

		// Use an optional binding here in case our client does not like to 
		// start persist service provided by this plugin
		bind(Properties.class).annotatedWith(Names.named("hibernate")).toProvider(Providers.<Properties>of(null));
		bind(NamingStrategy.class).to(ImprovedNamingStrategy.class);
		
		bind(PersistService.class).to(DefaultPersistService.class);
		bind(SessionFactory.class).toProvider(DefaultPersistService.class);
		bind(Configuration.class).toProvider(ConfigurationProvider.class);
		bind(UnitOfWork.class).to(DefaultUnitOfWork.class);
		bind(Session.class).toProvider(DefaultUnitOfWork.class);
		
		bind(Dao.class).to(DefaultDao.class);
		
	    TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
	    requestInjection(transactionInterceptor);
	    
	    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), transactionInterceptor);
	    
	    SessionInterceptor sessionInterceptor = new SessionInterceptor();
	    requestInjection(sessionInterceptor);
	    
	    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Sessional.class), sessionInterceptor);
	    
	    contribute(ObjectMapperConfigurator.class, HibernateObjectMapperConfigurator.class);
	    
	    contribute(ServletConfigurator.class, HibernateServletConfigurator.class);
	}

	@Override
	protected Class<? extends AbstractPlugin> getPluginClass() {
		return HibernatePlugin.class;
	}
	
}
