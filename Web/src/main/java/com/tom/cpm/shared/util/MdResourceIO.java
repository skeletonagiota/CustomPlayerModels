package com.tom.cpm.shared.util;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import com.tom.cpl.util.Image;
import com.tom.cpm.web.client.java.Java;
import com.tom.cpm.web.client.util.ImageIO;

import elemental2.dom.DomGlobal;
import elemental2.dom.FileReader;
import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;

public class MdResourceIO {
	private static final String IMAGES_ROOT = MdResourceLoader.RAW_WIKI_ROOT + "images/";

	public static CompletableFuture<Image> loadImage0(String url, boolean offline) {
		return MdResourceLoader.fetch(url, offline, true).thenCompose(ImageIO::loadImage);
	}

	public static CompletableFuture<byte[]> fetch0(String url) {
		if(url.startsWith(IMAGES_ROOT)) {
			return fetch("i:" + url.substring(IMAGES_ROOT.length()));
		} else if(url.startsWith(MdResourceLoader.RAW_WIKI_ROOT)) {
			return fetch("p:" + url.substring(MdResourceLoader.RAW_WIKI_ROOT.length()));
		} else if(url.startsWith("changelog")) {
			return fetch("c:c.md");
		}
		CompletableFuture<byte[]> cf = new CompletableFuture<>();
		cf.completeExceptionally(new IOException("Web editor cannot load websites"));
		return cf;
	}

	private static CompletableFuture<byte[]> fetch(String path) {
		CompletableFuture<byte[]> cf = new CompletableFuture<>();
		Java.promiseToCf(DomGlobal.fetch(System.getProperty("cpm.webApiEndpoint") + "/wiki?v=" + path).then(b -> {
			if (b.status != 200)return Promise.reject(b.statusText);
			return b.blob();
		}).then(blob -> new Promise<>((ResolveCallbackFn<String> res, RejectCallbackFn rej) -> {
			FileReader reader = new FileReader();
			reader.onloadend = __ -> {
				res.onInvoke(reader.result.asString());
				return null;
			};
			reader.onerror = __ -> {
				rej.onInvoke(null);
				return null;
			};
			reader.readAsDataURL(blob);
		})).then(dataUrl -> {
			String b64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
			return Promise.resolve(Base64.getDecoder().decode(b64));
		}), cf);
		return cf;
	}
}
