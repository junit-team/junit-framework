@import java.util.List
@import gg.jte.support.ForSupport
@import junitbuild.generator.model.JRE

@param List<JRE> supportedJres
@param String licenseHeader
${licenseHeader}
package org.junit.jupiter.api.condition;

public class JavaVersionPredicates {

	private static final int JAVA_VERSION = Runtime.version().feature();
@for(JRE jre : supportedJres)
	static boolean onJava${jre.getVersion()}() {
		return JAVA_VERSION == ${jre.getVersion()};
	}
@endfor
	static boolean onKnownVersion() {
		return @for(var jre : ForSupport.of(supportedJres))onJava${jre.get().getVersion()}()@if(!jre.isLast()) //
				|| @endif@endfor;
	}

	static boolean onOtherVersion() {
		return JRE.OTHER.isCurrentVersion();
	}

}
