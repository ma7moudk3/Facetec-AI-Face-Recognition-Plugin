package Processors;

public abstract class Processor {
    public abstract boolean isSuccess();

    public abstract String[] getBase64Images();

    public abstract String getErrorMessage();

    public abstract String getDocumentData();
}

