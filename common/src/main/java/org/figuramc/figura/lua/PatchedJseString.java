
package org.figuramc.figura.lua;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.luaj.vm2.Buffer;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.DumpState;
import org.luaj.vm2.lib.*;
import org.figuramc.figura.config.Configs;
import org.luaj.vm2.lib.jse.JseStringLib;
import org.luaj.vm2.lib.StringLib;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


public class PatchedJseString extends JseStringLib {

	public PatchedJseString() {
	}
	
	static final class rep extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			LuaString s = args.checkstring( 1 );
			int n = args.checkint( 2 );
			if (s.length() > Configs.STRING_REP_MAX_LENGTH.value) {
				return NIL;
			}
			if (n > Configs.STRING_REP_MAX_REPEATS.value) {
				return NIL;
			}
			final byte[] bytes = new byte[ s.length() * n ];
			int len = s.length();
			for ( int offset = 0; offset < bytes.length; offset += len ) {
				s.copyInto( 0, bytes, offset, len );
			}
			return LuaString.valueUsing( bytes );
		}
	}

	static Varargs str_find_aux( Varargs args, boolean find ) throws ClassNotFoundException, 
																NoSuchMethodException, IllegalAccessException, 
																InstantiationException, InvocationTargetException 
	{
		LuaString s = args.checkstring( 1 );
		LuaString pat = args.checkstring( 2 );
		int init = args.optint( 3, 1 );
		
		if ( init > 0 ) {
			init = Math.min( init - 1, s.length() );
		} else if ( init < 0 ) {
			init = Math.max( 0, s.length() + init );
		}
		
		boolean fastMatch = find && ( args.arg(4).toboolean() || pat.indexOfAny( SPECIALS ) == -1 );
		
		if ( fastMatch ) {
			int result = s.indexOf( pat, init );
			if ( result != -1 ) {
				return varargsOf( valueOf(result+1), valueOf(result+pat.length()) );
			}
		} else {
			Object ms = newMatchState( args, s, pat );
			
			boolean anchor = false;
			int poff = 0;
			if ( pat.length() > 0 && pat.luaByte( 0 ) == '^' ) {
				anchor = true;
				poff = 1;
			}
			
			int soff = init;
			int steps = 0;
			do {
				int res;
				ms.getClass().getDeclaredMethod("reset").invoke(ms);
				steps += 1;
				if (steps >= Configs.STRING_FIND_MAX_STEPS.value) {
				  return error("catastrophic backtracking?");
				}
				if ( ( res = (int) ms.getClass().getDeclaredMethod("match").invoke(ms, soff, poff) ) != -1 ) {
					if ( find ) {
						return varargsOf( valueOf(soff+1), valueOf(res), (Varargs) ms.getClass().getDeclaredMethod("push_captures").invoke(ms, false, soff, res));
					} else {
						return (Varargs) ms.getClass().getDeclaredMethod("push_captures").invoke(ms, true, soff, res);
					}
				}
			} while ( soff++ < s.length() && !anchor );
		}
		return NIL;
	}
	
	private static final LuaString SPECIALS = valueOf("^$*+?.([%-");
	
	static Object newMatchState(Varargs args, LuaString s, LuaString pat) throws ClassNotFoundException, 
																			NoSuchMethodException, IllegalAccessException, 
																			InstantiationException, InvocationTargetException 
	{
		Class<?> innerClass = Class.forName("org.luaj.vm2.lib.StringLib$MatchState");
		
		for (Method method : innerClass.getDeclaredMethods()) {
			method.setAccessible(true);
		}

		Constructor<?> constructor = innerClass.getDeclaredConstructor(Varargs.class, LuaString.class, LuaString.class);
		constructor.setAccessible(true);
		
		Object innerInstance = constructor.newInstance(args, s, pat);

		return innerInstance;
	}
	
}
