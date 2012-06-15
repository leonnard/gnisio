package net.gnisio.rebind;

import com.google.gwt.core.ext.GeneratorContextExt;
import com.google.gwt.core.ext.GeneratorExt;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.rebind.RebindResult;
import com.google.gwt.dev.javac.rebind.RebindStatus;


/**
 * Generator for producing the asynchronous version of a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 */
public class ServiceInterfaceProxyGenerator extends GeneratorExt {
 
  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContextExt ctx,
      String requestedClass) throws UnableToCompleteException {
    
    TypeOracle typeOracle = ctx.getTypeOracle();
    assert (typeOracle != null);

    JClassType remoteService = typeOracle.findType(requestedClass);
    if (remoteService == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
          + requestedClass + "'", null);
      throw new UnableToCompleteException();
    }

    if (remoteService.isInterface() == null) {
      logger.log(TreeLogger.ERROR, remoteService.getQualifiedSourceName()
          + " is not an interface", null);
      throw new UnableToCompleteException();
    }

    CustomProxyCreator proxyCreator = createProxyCreator(remoteService);

    TreeLogger proxyLogger = logger.branch(TreeLogger.DEBUG,
        "Generating client proxy for remote service interface '"
            + remoteService.getQualifiedSourceName() + "'", null);

    String returnTypeName = proxyCreator.create(proxyLogger, ctx);
    
    /*
     * Return with RebindStatus.USE_PARTIAL_CACHED, since we are implementing an
     * incremental scheme, which allows us to use a mixture of previously cached
     * and newly generated compilation units and artifacts.  For example, the
     * field serializers only need to be generated fresh if their source type
     * has changed (or if no previously cached version exists).
     */
    return new RebindResult(RebindStatus.USE_PARTIAL_CACHED, returnTypeName);
  }

  protected CustomProxyCreator createProxyCreator(JClassType remoteService) {
    return new CustomProxyCreator(remoteService);
  }
}