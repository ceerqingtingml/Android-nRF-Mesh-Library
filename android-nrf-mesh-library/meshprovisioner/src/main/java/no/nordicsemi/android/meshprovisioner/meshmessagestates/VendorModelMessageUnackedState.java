package no.nordicsemi.android.meshprovisioner.meshmessagestates;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import no.nordicsemi.android.meshprovisioner.InternalMeshMsgHandlerCallbacks;
import no.nordicsemi.android.meshprovisioner.messages.VendorModelMessageStatus;
import no.nordicsemi.android.meshprovisioner.messages.VendorModelMessageUnacked;
import no.nordicsemi.android.meshprovisioner.messagetypes.AccessMessage;
import no.nordicsemi.android.meshprovisioner.messagetypes.ControlMessage;
import no.nordicsemi.android.meshprovisioner.messagetypes.Message;
import no.nordicsemi.android.meshprovisioner.models.VendorModel;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class VendorModelMessageUnackedState extends GenericMessageState {

    private static final String TAG = VendorModelMessageUnackedState.class.getSimpleName();

    private final VendorModelMessageUnacked mVendorModelMessageUnacked;

    /**
     * Constructs {@link VendorModelMessageAckedState}
     *
     * @param context         Context of the application
     * @param dstAddress      Destination address to which the message must be sent to
     * @param vendorModelMessageUnacked Wrapper class {@link VendorModelMessageStatus} containing the opcode and parameters for {@link VendorModelMessageStatus} message
     * @param callbacks       {@link InternalMeshMsgHandlerCallbacks} for internal callbacks
     * @throws IllegalArgumentException
     */
    public VendorModelMessageUnackedState(@NonNull final Context context,
                                          @NonNull final byte[] dstAddress,
                                          @NonNull final VendorModelMessageUnacked vendorModelMessageUnacked,
                                          @NonNull final InternalMeshMsgHandlerCallbacks callbacks) throws IllegalArgumentException {
        super(context, dstAddress, vendorModelMessageUnacked.getMeshNode(), callbacks);
        this.mVendorModelMessageUnacked = vendorModelMessageUnacked;
        createAccessMessage();
    }

    @Override
    public MeshMessageState.MessageState getState() {
        return null;
    }

    /**
     * Creates the access message to be sent to the node
     */
    private void createAccessMessage() {
        final byte[] key = mVendorModelMessageUnacked.getAppKey();
        final int akf = mVendorModelMessageUnacked.getAkf();
        final int aid = mVendorModelMessageUnacked.getAid();
        final int aszmic = mVendorModelMessageUnacked.getAszmic();
        final int opCode = mVendorModelMessageUnacked.getOpCode();
        final byte[] parameters = mVendorModelMessageUnacked.getParameters();
        message = mMeshTransport.createVendorMeshMessage(mNode, (VendorModel) mMeshModel, mSrc, mDstAddress, key, akf, aid, aszmic, opCode, parameters);
        mPayloads.putAll(message.getNetworkPdu());
    }

    @Override
    public void executeSend() {
        Log.v(TAG, "Sending acknowledged vendor model message");
        super.executeSend();
    }

    @Override
    public boolean parseMeshPdu(final byte[] pdu) {
        final Message message = mMeshTransport.parsePdu(mSrc, pdu);
        if (message != null) {
            if (message instanceof AccessMessage) {
                final VendorModelMessageStatus vendorModelMessageStatus = new VendorModelMessageStatus(mNode, (AccessMessage) message);
                //TODO handle VendorModelMessageStatus message
                mInternalTransportCallbacks.updateMeshNode(mNode);
            } else {
                parseControlMessage((ControlMessage) message, mPayloads.size());
                return true;
            }
        } else {
            Log.v(TAG, "Message reassembly may not be complete yet");
        }
        return false;
    }

    @Override
    public void sendSegmentAcknowledgementMessage(final ControlMessage controlMessage) {
        final ControlMessage message = mMeshTransport.createSegmentBlockAcknowledgementMessage(controlMessage);
        Log.v(TAG, "Sending acknowledgement: " + MeshParserUtils.bytesToHex(message.getNetworkPdu().get(0), false));
        mInternalTransportCallbacks.sendPdu(mNode, message.getNetworkPdu().get(0));
        mMeshStatusCallbacks.onBlockAcknowledgementSent(mNode);
    }
}
