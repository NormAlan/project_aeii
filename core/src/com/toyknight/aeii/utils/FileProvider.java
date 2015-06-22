package com.toyknight.aeii.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Created by toyknight on 4/4/2015.
 */
public class FileProvider {

    private static Platform platform = Platform.Desktop;

    private FileProvider() {
    }

    public static void setPlatform(Platform platform) {
        FileProvider.platform = platform;
    }

    public static FileHandle getAssetsFile(String path) {
        switch (platform) {
            case Android:
                return Gdx.files.internal(path);
            case iOS:
                return Gdx.files.internal(path);
            case Desktop:
            default:
                return Gdx.files.local("assets/" + path);
        }
    }

    public static FileHandle getUserFile(String path) {
        return Gdx.files.local("user/" + path);
    }

    public static FileHandle getUserDir(String path) {
        FileHandle dir = Gdx.files.local("user/" + path);
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        } else {
            dir.mkdirs();
            return dir;
        }
    }

}
