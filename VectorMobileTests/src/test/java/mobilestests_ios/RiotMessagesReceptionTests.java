package mobilestests_ios;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.esotericsoftware.yamlbeans.YamlException;

import io.appium.java_client.MobileElement;
import pom_ios.RiotRoomPageObjects;
import pom_ios.main_tabs.RiotHomePageTabObjects;
import pom_ios.main_tabs.RiotRoomsTabPageObjects;
import utility.Constant;
import utility.HttpsRequestsToMatrix;
import utility.ReadConfigFile;
import utility.RiotParentTest;
import utility.ScreenshotUtility;

@Listeners({ ScreenshotUtility.class })
public class RiotMessagesReceptionTests extends RiotParentTest{
	private String msgFromUpUser="UP";
	
	private String roomId="!SBpfTGBlKgELgoLALQ%3Amatrix.org";
	private String roomIdCustomHs="!LVRuDkmtSvMXfqSgLy%3Ajeangb.org";
	
	private String pictureURL="mxc://matrix.org/gpQYPbjoqVeTWCGivjRshIni";
	private String pictureURLCustomHs="mxc://jeangb.org/mQULDSeUacWtxnGlSNBofySw";
	
	private String roomTest="msg rcpt 4 automated tests";
	private String riotUserDisplayNameA="riotuser4";
	private String riotUserDisplayNameB="riotuser5";
	private String riotSenderUserDisplayName="riotuserup";
	private String riotSenderAccessToken;

	/**
	 * Validates issue https://github.com/vector-im/riot-ios/issues/809 </br>
	 * 1. Open roomtest with device A. </br>
	 * 2. Open roomtest with device B. </br>
	 * 3. User A write something in the message bar but don't send it. </br>
	 * Test that the typing indicator indicates '[user1] is typing..." with device B. </br>
	 * 4. Type an other msg and clear it with user 4 in the message bar. </br>
	 * Test that the typing indicator is empty on device B. </br>
	 * @throws InterruptedException 
	 */
	@Test(groups={"2drivers_ios","2checkuser"})
	public void typingIndicatorTest() throws InterruptedException{
		String notSentMsg="tmp";
		RiotHomePageTabObjects homePageA = new RiotHomePageTabObjects(appiumFactory.getiOsDriver1());
		RiotHomePageTabObjects homePageB = new RiotHomePageTabObjects(appiumFactory.getiOsDriver2());

		//1. Open roomtest with device A.
		homePageA.getRoomByName(roomTest).click();
		RiotRoomPageObjects roomA=new  RiotRoomPageObjects(appiumFactory.getiOsDriver1());

		//2. Open roomtest with device B.		
		homePageB.getRoomByName(roomTest).click();
		RiotRoomPageObjects roomB=new  RiotRoomPageObjects(appiumFactory.getiOsDriver2());

		//3. User A write something in the message bar but don't send it.
		roomA.sendKeyTextView.setValue(notSentMsg);
		//Test that the typing indicator indicates '[user1] is typing..." with device B.
		Assert.assertEquals(roomB.notificationMessage.getText(), riotUserDisplayNameA+" is typing...");
		Assert.assertTrue(roomB.notificationMessage.isDisplayed(),"Typing indicator isn't displayed on device B");

		//4. Type an other msg and clear it with user 4 in the message bar.
		roomA.sendKeyTextView.setValue(notSentMsg);
		roomA.sendKeyTextView.findElementByClassName("XCUIElementTypeTextView").clear();
		//Test that the typing indicator is empty on device B.
		Assert.assertFalse(roomB.notificationMessage.isDisplayed(),"Typing indicator is displayed on device B and shouldn't because device A isn't typing");
		//come back to rooms list
		roomA.menuBackButton.click();
		roomB.menuBackButton.click();
	}

	/**
	 * 1. Stay in recents list and get the current badge on room roomTest </br>
	 * 2. Receive a message in a room from an other user. </br>
	 * Asserts that badge is set to 1 or incremented on the room's item in the rooms list.</br>
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test(groups={"messageReceivedInList","1checkuser","1driver_ios"},priority=1)
	public void checkBadgeAndMessageOnRoomItem() throws InterruptedException, IOException{
		//1. Stay in recents list and get the current badge on room roomTest
		RiotHomePageTabObjects homePage = new RiotHomePageTabObjects(appiumFactory.getiOsDriver1());
		RiotRoomsTabPageObjects roomsTabPage= homePage.openRoomsTab();
		Integer currentBadge=roomsTabPage.getBadgeNumberByRoomName(roomTest);

		//2. Receive a message in a room from an other user.
		HttpsRequestsToMatrix.sendMessageInRoom(riotSenderAccessToken, getRoomId(), msgFromUpUser);
		if(currentBadge==null)currentBadge=0;
		//wait until message is received
		roomsTabPage.waitForRoomToReceiveNewMessage(roomTest, currentBadge);
		//Asserts that badge is set to 1 or incremented on the room's item in the rooms list
		Assert.assertNotNull(roomsTabPage.getBadgeNumberByRoomName(roomTest), "There is no badge on this room.");
		Assert.assertEquals((int)roomsTabPage.getBadgeNumberByRoomName(roomTest),currentBadge+1, "Badge number wasn't incremented after receiving the message");	
		//Assertion on the message.
		Assert.assertEquals(roomsTabPage.getLastEventByRoomName(roomTest,false), msgFromUpUser, "Received message on the room item isn't the same as sended by matrix.");
	}

	/**
	 * TODO : write this test
	 * Required : user must be logged in room </br>
	 * Set the notifications off on the room </br>
	 * Receive a text message in a room from an other user. </br>
	 * Asserts that no badge appears after receiving the message.</br>
	 */
	@Test(enabled=false)
	public void checkNoBadgeOnMessageReceptionWithNootificationsOff(){

	}
	/**
	 * Receive a text message in a room from an other user. </br>
	 * 1. Open room roomName </br>
	 * Asserts that badge is set to 1 or incremented on the room's item in the rooms list.</br>
	 * Asserts that badge isn't displayed anymore on the room item when going back to rooms list.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test(dependsOnGroups="messageReceivedInList",priority=2,groups={"roomOpenned","1checkuser","1driver_ios"})
	public void checkTextMessageOnRoomPage() throws InterruptedException{
		//1. Open room roomName
		RiotRoomsTabPageObjects roomsTabPage = new RiotRoomsTabPageObjects(appiumFactory.getiOsDriver1());
		roomsTabPage.getRoomByName(roomTest).click();
		//check that lately sended message is the last displayed in the room
		RiotRoomPageObjects testRoom = new RiotRoomPageObjects(appiumFactory.getiOsDriver1());
		MobileElement lastPost= testRoom.getLastBubble();
		Assert.assertTrue(testRoom.getTextViewFromBubble(lastPost).getText().contains(msgFromUpUser), "Last bubble doesn't contains msg sent by riotuserup.");
	}

	/**
	 * 1. Send message in a room.</br>
	 * Check that timestamp is displayed on the last post.</br>
	 * Check that timestamp is not displayed on the before last post.</br>
	 * 2. Select an other post that the last </br>
	 * Check that when a post is selected, timestamp is displayed.
	 * @throws InterruptedException 
	 */
	@Test(dependsOnGroups="roomOpenned",groups={"1checkuser","1driver_ios"},priority=3)
	public void checkTimeStampPositionOnRoomPage() throws InterruptedException{
		String message="test for timestamp display";
		RiotRoomPageObjects testRoom = new RiotRoomPageObjects(appiumFactory.getiOsDriver1());
		//1. Send message in a room.
		testRoom.sendAMessage(message);Thread.sleep(500);
		appiumFactory.getiOsDriver1().hideKeyboard();
		//Check that timestamp is displayed on the last post.
		MobileElement lastPost= testRoom.getLastBubble();
		Assert.assertNotNull(testRoom.getTimeStampByBubble(lastPost), "Last message have no timestamp");
		Assert.assertTrue(testRoom.getTimeStampByBubble(lastPost).getText().length()>=5, "Last message timestamp seems bad.");

		//Check that timestamp is not displayed on the before last post.
		int beforeLastPostPosition=testRoom.bubblesList.size()-2;
		MobileElement beforeLastPost=  testRoom.bubblesList.get(beforeLastPostPosition);
		Assert.assertNull(testRoom.getTimeStampByBubble(beforeLastPost), "Before last message have timestamp and should not.");

		//2. Select an other post that the last 
		testRoom.getTextViewFromBubble(beforeLastPost).click();
		//Check that when a post is selected, timestamp is displayed.
		Assert.assertNotNull(testRoom.getTimeStampByBubble(beforeLastPost), "Before last message have no timestamp");
		Assert.assertTrue(testRoom.getTimeStampByBubble(beforeLastPost).getText().length()>=5, "Before last message timestamp seems bad.");
	}

	/**
	 * TODO
	 * Check that a thumbnail is present when a photo is uploaded, instead of the full resolution photo.
	 */
	@Test(enabled=false)
	public void checkUploadedPhotoThumbnail(){

	}

	private String getRoomId() {
		try {
			if("false".equals(ReadConfigFile.getInstance().getConfMap().get("homeserverlocal"))){
				return roomId;
			}else{
				return roomIdCustomHs;
			}
		} catch (FileNotFoundException | YamlException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	@SuppressWarnings("unused")
	private String getPictureURL() {
		try {
			if("false".equals(ReadConfigFile.getInstance().getConfMap().get("homeserverlocal"))){
				return pictureURL;
			}else{
				return pictureURLCustomHs;
			}
		} catch (FileNotFoundException | YamlException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@BeforeGroups("1checkuser")
	private void checkIfUser1Logged() throws InterruptedException, FileNotFoundException, YamlException{
		super.checkIfUserLoggedAndHomeServerSetUpIos(appiumFactory.getiOsDriver1(), riotUserDisplayNameA, Constant.DEFAULT_USERPWD);
	}
	/**
	 * Log the good user if not.</br> Secure the test.
	 * @param myDriver
	 * @param username
	 * @param pwd
	 * @throws InterruptedException 
	 * @throws YamlException 
	 * @throws FileNotFoundException 
	 */
	@BeforeGroups("2checkuser")
	private void checkIfUsersLogged() throws InterruptedException, FileNotFoundException, YamlException{
		super.checkIfUserLoggedAndHomeServerSetUpIos(appiumFactory.getiOsDriver1(), riotUserDisplayNameA, Constant.DEFAULT_USERPWD);
		super.checkIfUserLoggedAndHomeServerSetUpIos(appiumFactory.getiOsDriver2(), riotUserDisplayNameB, Constant.DEFAULT_USERPWD);
	}

	/**
	 * Log riotuserup to get his access token. </br> Mandatory to send http request with it.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@BeforeGroups("1checkuser")
	private void renewRiotInviterAccessToken() throws IOException, InterruptedException{
		System.out.println("Log "+riotSenderUserDisplayName+" to get a new AccessToken.");
		riotSenderAccessToken=HttpsRequestsToMatrix.login(riotSenderUserDisplayName, Constant.DEFAULT_USERPWD);
	}
}
