package com.project.client;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Longs;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.validation.client.impl.Validation;
import com.project.shared.GreetingService;
import com.project.shared.GreetingServiceAsync;
import com.project.shared.entities.GreetingResponse;
import com.project.shared.entities.User;
import com.seanchenxi.gwt.storage.client.StorageExt;
import com.seanchenxi.gwt.storage.client.StorageKey;
import com.seanchenxi.gwt.storage.client.StorageKeyProvider;

public class View extends Composite {

  interface MyStorageKeyProvider extends StorageKeyProvider {
    @Key("STORAGE_USERS_KEY")
    StorageKey<StoredUsers> key();
  }

  interface ViewUiBinder extends UiBinder<Widget, View> {
  }

  private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
  private static final MyStorageKeyProvider KEY_PROVIDER = GWT.create(MyStorageKeyProvider.class);
  private static final Logger logger = Logger.getLogger("");
  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
  @UiField
  FlowPanel loggingPanel;
  @UiField
  TextBox nameTextBox;
  @UiField
  Button saveBtn;
  @UiField
  InlineLabel lastCreatedUserLabel;
  @UiField
  Button storeBtn;
  @UiField
  HTMLPanel databaseUsersPanel;
  @UiField
  Button databaseReloadBtn;
  @UiField
  Button databaseClearBtn;
  @UiField
  HTMLPanel localStorageUsersPanel;
  @UiField
  Button localStorageReloadBtn;
  @UiField
  Button localStorageClearBtn;
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

  private User lastCreatedUser;

  private final StorageExt localStorage = StorageExt.getLocalStorage();

  public View() {
    initWidget(uiBinder.createAndBindUi(this));
    logger.addHandler(new HasWidgetsLogHandler(loggingPanel));

    nameTextBox.getElement().setAttribute("placeholder", "User's name");

    saveBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        processUsername();
      }
    });
    nameTextBox.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
          processUsername();
        }
      }
    });

    storeBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (lastCreatedUser == null) {
          logger.warning("No User has been created yet.");
        } else {
          localStore(lastCreatedUser);
          localStorageReloadUsers();
        }
      }
    });

    databaseReloadBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        databaseReloadUsers();
      }
    });
    databaseReloadUsers();

    databaseClearBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        greetingService.clearUsers(new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.log(Level.SEVERE, "Database hasn't been cleared.", caught);
          }

          @Override
          public void onSuccess(Void result) {
            logger.info("Database has been cleared.");
            databaseReloadUsers();
          }
        });
      }
    });

    localStorageReloadBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        localStorageReloadUsers();
      }
    });
    localStorageReloadUsers();

    localStorageClearBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (localStorage != null) {
          if (localStorage.containsKey(KEY_PROVIDER.key())) {
            try {
              localStorage.remove(KEY_PROVIDER.key());
              logger.info("Local Storage has been cleared.");
              localStorageReloadUsers();
            } catch (Exception e) {
              logger.log(Level.SEVERE, "Local Storage hasn't been cleared.", e);
            }
          }
        }
      }
    });
  }

  private void databaseReloadUsers() {
    databaseUsersPanel.clear();
    greetingService.getUsers(new AsyncCallback<List<User>>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.log(Level.SEVERE, "Reload Users from Database has failed.", caught);
      }

      @Override
      public void onSuccess(List<User> users) {
        logger.info("Reload Users from Database has succeeded.");
        for (User user : users) {
          databaseUsersPanel.add(new HTMLPanel("li", user.getName()));
        }
      }
    });
  }

  private void initLocalStorage() {
    if (localStorage != null) {
      if (!localStorage.containsKey(KEY_PROVIDER.key())) {
        try {
          final StoredUsers storedUsers = new StoredUsers();
          localStorage.put(KEY_PROVIDER.key(), storedUsers);
          logger.info("Local Storage has been initialized.");
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Local Storage hasn't been initialized.", e);
        }
      }
    }
  }

  private void localStorageReloadUsers() {
    localStorageUsersPanel.clear();
    if (localStorage != null) {
      if (localStorage.containsKey(KEY_PROVIDER.key())) {
        try {
          final StoredUsers storedUsers = localStorage.get(KEY_PROVIDER.key());
          for (User user : storedUsers.getUsers()) {
            localStorageUsersPanel.add(new HTMLPanel("li", user.getName()));
          }
          logger.info("Reload Users from Local Storage has succeeded.");
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Reload Users from Local Storage has failed.", e);
        }
      }
    }
  }

  private void localStore(User user) {
    if (localStorage != null) {
      initLocalStorage();
      try {
        final StoredUsers storedUsers = localStorage.get(KEY_PROVIDER.key());
        final boolean added = storedUsers.getUsers().add(user);
        localStorage.put(KEY_PROVIDER.key(), storedUsers);
        if (added) {
          logger.info(user.getName() + " has been stored in Local Storage.");
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, user.getName() + " hasn't been stored in Local Storage.", e);
      }
    }
  }

  private void processUsername() {
    Set<ConstraintViolation<User>> violations =
        VALIDATOR.validateValue(User.class, "name", nameTextBox.getText().trim(), Default.class);
    if (violations.isEmpty()) {
      greetingService.greetServer(nameTextBox.getText().trim(), new AsyncCallback<GreetingResponse>() {
        @Override
        public void onFailure(Throwable caught) {
          logger.log(Level.SEVERE, "greetServer has thrown an Exception.", caught);
        }

        @Override
        public void onSuccess(GreetingResponse greetingResponse) {
          final User user = greetingResponse.getUserRef().get();
          if (greetingResponse.getCount() == 0) {
            logger.info("A new User name:" + user.getName() + " id:" + user.getId() + " has been saved.");
          }
          // Guava GWT works well in front-end code
          final String idBase32Encoded = BaseEncoding.base32().encode(Longs.toByteArray(user.getId()));
          logger.info("User id:" + user.getId() + " base32 encoded:" + idBase32Encoded);

          final GreetingDialogBox greetingDialogBox = new GreetingDialogBox(greetingResponse);
          greetingDialogBox.asDialogBox().addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
              nameTextBox.setFocus(true);
            }
          });
          greetingDialogBox.center();

          nameTextBox.setText(null);
          setLastCreatedUser(user);
          databaseReloadUsers();
        }
      });
    } else {
      for (ConstraintViolation<User> constraintViolation : violations) {
        logger.warning(constraintViolation.getMessage());
      }
    }
  }

  private void setLastCreatedUser(User user) {
    lastCreatedUser = user;
    lastCreatedUserLabel.setText(user.getName());
  }
}
